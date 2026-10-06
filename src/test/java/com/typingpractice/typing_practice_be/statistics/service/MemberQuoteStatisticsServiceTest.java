package com.typingpractice.typing_practice_be.statistics.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.service.YesterdayBatchStatusCache.Mode;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberDailyAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypingAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypoAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberDailyStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypingSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberDailyStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoDetailStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.TodayTypingStatsRedisService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class MemberQuoteStatisticsServiceTest {
  private static final Long MEMBER_ID = 1L;
  private static final QuoteLanguage LANG = QuoteLanguage.KOREAN;

  @Mock private TypingRecordRepository typingRecordRepository;
  @Mock private MemberTypingAggregationRepository memberTypingAggregationRepository;
  @Mock private MemberDailyAggregationRepository memberDailyAggregationRepository;
  @Mock private MemberTypoAggregationRepository memberTypoAggregationRepository;
  @Mock private MemberTypingStatsRepository memberTypingStatsRepository;
  @Mock private MemberDailyStatsRepository memberDailyStatsRepository;
  @Mock private MemberTypoStatsRepository memberTypoStatsRepository;
  @Mock private MemberTypoDetailStatsRepository memberTypoDetailStatsRepository;
  @Mock private TodayTypingStatsRedisService todayTypingStatsRedisService;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOps;
  @Mock private YesterdayBatchStatusCache yesterdayBatchStatusCache;

  @InjectMocks private MemberQuoteStatisticsService service;

  @BeforeEach
  void setUp() {
    when(todayTypingStatsRedisService.getTyping(MEMBER_ID, LANG))
        .thenReturn(TodayTypingSnapshot.empty());
  }

  @Test
  @DisplayName("완료 마커가 있으면 PG/Mongo 판정 조회 없이 어제 집계를 건너뛴다")
  void cachedDone_skipsLookup() {
    when(yesterdayBatchStatusCache.isDone(Mode.QUOTE, MEMBER_ID, LANG)).thenReturn(true);

    service.getTypingStats(MEMBER_ID, LANG);

    verifyNoInteractions(
        memberDailyStatsRepository, typingRecordRepository, memberTypingAggregationRepository);
    verify(yesterdayBatchStatusCache, never()).markDone(any(), any(), any());
  }

  @Test
  @DisplayName("PG에 어제 통계가 있으면 완료 마커를 저장하고 Mongo를 조회하지 않는다")
  void pgPresent_marksDone() {
    when(memberDailyStatsRepository.findByMemberIdAndDateAndLanguage(
            eq(MEMBER_ID), any(LocalDate.class), eq(LANG)))
        .thenReturn(Optional.of(mock(MemberDailyStats.class)));

    service.getTypingStats(MEMBER_ID, LANG);

    verify(yesterdayBatchStatusCache).markDone(Mode.QUOTE, MEMBER_ID, LANG);
    verifyNoInteractions(typingRecordRepository, memberTypingAggregationRepository);
  }

  @Test
  @DisplayName("PG에 없고 Mongo에도 어제 기록이 없으면 완료 마커를 저장한다")
  void noYesterdayRecords_marksDone() {
    when(typingRecordRepository.existsByMemberIdBetween(
            eq(MEMBER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(false);

    service.getTypingStats(MEMBER_ID, LANG);

    verify(yesterdayBatchStatusCache).markDone(Mode.QUOTE, MEMBER_ID, LANG);
    verifyNoInteractions(memberTypingAggregationRepository);
  }

  @Test
  @DisplayName("배치 미완료(어제 기록 있음)이면 마커를 저장하지 않고 Mongo 집계를 수행한다")
  void pending_doesNotMarkDone() {
    when(typingRecordRepository.existsByMemberIdBetween(
            eq(MEMBER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true);

    service.getTypingStats(MEMBER_ID, LANG);

    verify(yesterdayBatchStatusCache, never()).markDone(any(), any(), any());
    verify(memberTypingAggregationRepository)
        .aggregateByMemberIdsAndLanguageBetween(
            eq(List.of(MEMBER_ID)), eq(LANG), any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("refreshStats는 모든 언어의 완료 마커를 삭제한다")
  void refresh_clearsMarkers() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);

    service.refreshStats(MEMBER_ID, LANG);

    verify(yesterdayBatchStatusCache).clear(Mode.QUOTE, MEMBER_ID, QuoteLanguage.values());
  }
}
