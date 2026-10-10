package com.typingpractice.typing_practice_be.typingrecord.statistics.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypingAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypoAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypingSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailEntry;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoSnapshot;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TodayTypingStatsRedisServiceTest {
  private static final Long MEMBER_ID = 1L;
  private static final String TYPING_KEY = "today:typing:1:KOREAN";
  private static final String TYPO_KEY = "today:typo:1";
  private static final String TYPO_DETAIL_KEY = "today:typo-detail:1";

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOps;
  @Mock private HashOperations<String, Object, Object> hashOps;
  @Mock private MemberTypingAggregationRepository typingAggregationRepository;
  @Mock private MemberTypoAggregationRepository typoAggregationRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private TodayTypingStatsRedisService service;

  @BeforeEach
  void setUp() {
    service =
        new TodayTypingStatsRedisService(
            redisTemplate, objectMapper, typingAggregationRepository, typoAggregationRepository);
  }

  @Test
  @DisplayName("getTyping: 캐시 미스 + 오늘 기록 없음 → 빈 스냅샷을 Redis에 저장한다")
  void getTyping_cachesEmptySnapshot() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.get(TYPING_KEY)).thenReturn(null);
    when(typingAggregationRepository.aggregateByMemberIdsAndLanguageBetween(
            anyList(), eq(QuoteLanguage.KOREAN), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(List.of());

    TodayTypingSnapshot result = service.getTyping(MEMBER_ID, QuoteLanguage.KOREAN);

    assertThat(result.getTotalAttempts()).isZero();
    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(valueOps).set(eq(TYPING_KEY), json.capture(), any(Duration.class));
    assertThat(json.getValue()).contains("\"totalAttempts\":0");
  }

  @Test
  @DisplayName("getTypo: 캐시 미스 + 오늘 오타 없음 → 빈 스냅샷을 Redis에 저장한다")
  void getTypo_cachesEmptySnapshot() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.get(TYPO_KEY)).thenReturn(null);
    when(typoAggregationRepository.aggregateByMemberIdsBetween(
            anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(List.of());

    TodayTypoSnapshot result = service.getTypo(MEMBER_ID);

    assertThat(result.getTypoCountMap()).isEmpty();
    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(valueOps).set(eq(TYPO_KEY), json.capture(), any(Duration.class));
    assertThat(json.getValue()).contains("\"typoCountMap\":{}");
  }

  @Test
  @DisplayName("getTypoDetail: 캐시 미스 + 오늘 오타 없음 → 센티널 필드를 Hash에 저장하고 TTL을 건다")
  void getTypoDetail_cachesEmptyMarker() {
    when(redisTemplate.opsForHash()).thenReturn(hashOps);
    when(hashOps.entries(TYPO_DETAIL_KEY)).thenReturn(Map.of());
    when(typoAggregationRepository.aggregateByMemberIdsBetween(
            anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(List.of());
    when(redisTemplate.getExpire(TYPO_DETAIL_KEY)).thenReturn(-2L);

    TodayTypoDetailSnapshot result = service.getTypoDetail(MEMBER_ID);

    assertThat(result.getDetailMap()).isEmpty();
    verify(hashOps).put(eq(TYPO_DETAIL_KEY), eq("_empty"), anyString());
    verify(redisTemplate).expire(eq(TYPO_DETAIL_KEY), any(Duration.class));
  }

  @Test
  @DisplayName("getTypoDetail: 센티널만 있는 Hash → Mongo 집계 없이 빈 스냅샷을 반환한다")
  void getTypoDetail_sentinelOnly_skipsAggregation() {
    when(redisTemplate.opsForHash()).thenReturn(hashOps);
    when(hashOps.entries(TYPO_DETAIL_KEY)).thenReturn(Map.of("_empty", ""));

    TodayTypoDetailSnapshot result = service.getTypoDetail(MEMBER_ID);

    assertThat(result.getDetailMap()).isEmpty();
    verifyNoInteractions(typoAggregationRepository);
  }

  @Test
  @DisplayName("getTypoDetail: 센티널과 실제 항목이 함께 있으면 실제 항목만 반환한다")
  void getTypoDetail_sentinelWithEntries_returnsOnlyEntries() throws Exception {
    String field = TodayTypoDetailSnapshot.toKey(QuoteLanguage.KOREAN, "ㄱ", "ㄴ");
    String entryJson = objectMapper.writeValueAsString(TodayTypoDetailEntry.create(3, 1, 1, 1, 0));
    when(redisTemplate.opsForHash()).thenReturn(hashOps);
    when(hashOps.entries(TYPO_DETAIL_KEY)).thenReturn(Map.of("_empty", "", field, entryJson));

    TodayTypoDetailSnapshot result = service.getTypoDetail(MEMBER_ID);

    assertThat(result.getDetailMap()).containsOnlyKeys(field);
    assertThat(result.getDetailMap().get(field).getCount()).isEqualTo(3);
  }

  @Test
  @DisplayName("getTypoDetailByLanguage: 정답 글자와 관계없이 요청한 언어의 항목만 반환한다")
  void getTypoDetailByLanguage_returnsOnlyRequestedLanguage() throws Exception {
    String entryJson = objectMapper.writeValueAsString(TodayTypoDetailEntry.create(1, 0, 0, 0, 1));
    when(redisTemplate.opsForHash()).thenReturn(hashOps);
    when(hashOps.entries(TYPO_DETAIL_KEY))
        .thenReturn(
            Map.of(
                "KOREAN:ㄱ:ㄴ", entryJson,
                "KOREAN: :", entryJson,
                "ENGLISH: :x", entryJson));

    TodayTypoDetailSnapshot result =
        service.getTypoDetailByLanguage(MEMBER_ID, QuoteLanguage.KOREAN);

    assertThat(result.getDetailMap()).containsOnlyKeys("KOREAN:ㄱ:ㄴ", "KOREAN: :");
  }
}
