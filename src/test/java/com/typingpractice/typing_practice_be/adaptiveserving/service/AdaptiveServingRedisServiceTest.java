package com.typingpractice.typing_practice_be.adaptiveserving.service;

import org.springframework.data.redis.core.ValueOperations;
import java.util.Optional;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveServingEstimation;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.adaptiveserving.config.AdaptiveServingProperties;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class AdaptiveServingRedisServiceTest {
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ObjectMapper objectMapper;
  @Mock private AdaptiveServingCalculator calculator;
  @Mock private AdaptiveServingProperties properties;
  @Mock private MemberTypingStatsRepository memberTypingStatsRepository;
  @Mock private TypingRecordRepository typingRecordRepository;
  @Mock private Cursor<String> cursor;
  @Mock private ValueOperations<String, String> valueOps;

  @InjectMocks private AdaptiveServingRedisService redisService;

  @Test
  @DisplayName("deleteAllEstimations: SCAN 으로 모은 adaptive:* 키를 한 번에 삭제하고 커서를 닫는다")
  void deletesAllScannedKeys() {
    // given
    when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
    when(cursor.hasNext()).thenReturn(true, true, false);
    when(cursor.next()).thenReturn("adaptive:1:KOREAN", "adaptive:2:KOREAN");
    List<String> expectedKeys = List.of("adaptive:1:KOREAN", "adaptive:2:KOREAN");
    when(redisTemplate.delete(expectedKeys)).thenReturn(2L);

    // when
    long deleted = redisService.deleteAllEstimations();

    // then
    assertThat(deleted).isEqualTo(2L);
    verify(redisTemplate).delete(expectedKeys);
    verify(cursor).close();
  }

  @Test
  @DisplayName("deleteAllEstimations: 키가 없으면 delete 를 호출하지 않고 0 을 돌려준다")
  void returnsZeroWhenNoKeys() {
    // given
    when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
    when(cursor.hasNext()).thenReturn(false);

    // when
    long deleted = redisService.deleteAllEstimations();

    // then
    assertThat(deleted).isZero();
    verify(redisTemplate, never()).delete(anyCollection());
    verify(cursor).close();
  }

  @Test
  @DisplayName("getEstimation: PostgreSQL 에 μ 0 / σ 5.3 이 저장된 회원은 기본값(50)이 아니라 0 을 돌려준다 (μ 0 은 클램프로 생기는 유효값)")
  void zeroMuFromPostgresIsAValidEstimate() throws Exception {
    // given — Redis miss, PostgreSQL 확정값 (0, 5.3), 오늘 기록 없음
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.get(anyString())).thenReturn(null);
    MemberTypingStats stats =
        MemberTypingStats.create(null, QuoteLanguage.KOREAN, 10, 130f, 0.9f, 150, 5f, 0, null);
    stats.updateEstimation(0f, 5.3f);
    when(memberTypingStatsRepository.findByMemberIdAndLanguage(1L, QuoteLanguage.KOREAN))
        .thenReturn(Optional.of(stats));
    when(typingRecordRepository.findForAdaptiveServingBetween(eq(1L), eq(QuoteLanguage.KOREAN), any(), any()))
        .thenReturn(List.of());
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    // 수정 전에는 μ 0 을 "없음" 으로 보고 기본값을 쓴다 → 50 이 나와 실패해야 한다
    lenient().when(properties.getDefaultMu()).thenReturn(50f);
    lenient().when(properties.getDefaultSigma()).thenReturn(25f);

    // when
    AdaptiveServingEstimation estimation = redisService.getEstimation(1L, QuoteLanguage.KOREAN);

    // then
    assertThat(estimation.getMu()).isEqualTo(0f);
    assertThat(estimation.getSigma()).isEqualTo(5.3f);
  }
}
