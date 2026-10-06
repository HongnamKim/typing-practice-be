package com.typingpractice.typing_practice_be.statistics.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.statistics.service.YesterdayBatchStatusCache.Mode;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.WordTypingRecordRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberDailyWordAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypingAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypoAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberDailyWordStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypingSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberDailyWordStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypingStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypoDetailStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypoStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service.TodayWordTypingStatsRedisService;
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
class MemberWordStatisticsServiceTest {
  private static final Long MEMBER_ID = 1L;
  private static final WordLanguage LANG = WordLanguage.KOREAN;

  @Mock private WordTypingRecordRepository wordTypingRecordRepository;
  @Mock private MemberWordTypingAggregationRepository memberWordTypingAggregationRepository;
  @Mock private MemberDailyWordAggregationRepository memberDailyWordAggregationRepository;
  @Mock private MemberWordTypoAggregationRepository memberWordTypoAggregationRepository;
  @Mock private MemberWordTypingStatsRepository memberWordTypingStatsRepository;
  @Mock private MemberDailyWordStatsRepository memberDailyWordStatsRepository;
  @Mock private MemberWordTypoStatsRepository memberWordTypoStatsRepository;
  @Mock private MemberWordTypoDetailStatsRepository memberWordTypoDetailStatsRepository;
  @Mock private TodayWordTypingStatsRedisService todayWordTypingStatsRedisService;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOps;
  @Mock private YesterdayBatchStatusCache yesterdayBatchStatusCache;

  @InjectMocks private MemberWordStatisticsService service;

  @BeforeEach
  void setUp() {
    when(todayWordTypingStatsRedisService.getTyping(MEMBER_ID, LANG))
        .thenReturn(TodayWordTypingSnapshot.empty());
  }

  @Test
  @DisplayName("완료 마커가 있으면 PG/Mongo 판정 조회 없이 어제 집계를 건너뛴다")
  void cachedDone_skipsLookup() {
    when(yesterdayBatchStatusCache.isDone(Mode.WORD, MEMBER_ID, LANG)).thenReturn(true);

    service.getTypingStats(MEMBER_ID, LANG);

    verifyNoInteractions(
        memberDailyWordStatsRepository,
        wordTypingRecordRepository,
        memberWordTypingAggregationRepository);
    verify(yesterdayBatchStatusCache, never()).markDone(any(), any(), any());
  }

  @Test
  @DisplayName("PG에 어제 통계가 있으면 완료 마커를 저장하고 Mongo를 조회하지 않는다")
  void pgPresent_marksDone() {
    when(memberDailyWordStatsRepository.findByMemberIdAndDateAndLanguage(
            eq(MEMBER_ID), any(LocalDate.class), eq(LANG)))
        .thenReturn(Optional.of(mock(MemberDailyWordStats.class)));

    service.getTypingStats(MEMBER_ID, LANG);

    verify(yesterdayBatchStatusCache).markDone(Mode.WORD, MEMBER_ID, LANG);
    verifyNoInteractions(wordTypingRecordRepository, memberWordTypingAggregationRepository);
  }

  @Test
  @DisplayName("PG에 없고 Mongo에도 어제 기록이 없으면 완료 마커를 저장한다")
  void noYesterdayRecords_marksDone() {
    when(wordTypingRecordRepository.existsByMemberIdBetween(
            eq(MEMBER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(false);

    service.getTypingStats(MEMBER_ID, LANG);

    verify(yesterdayBatchStatusCache).markDone(Mode.WORD, MEMBER_ID, LANG);
    verifyNoInteractions(memberWordTypingAggregationRepository);
  }

  @Test
  @DisplayName("배치 미완료(어제 기록 있음)이면 마커를 저장하지 않고 Mongo 집계를 수행한다")
  void pending_doesNotMarkDone() {
    when(wordTypingRecordRepository.existsByMemberIdBetween(
            eq(MEMBER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true);

    service.getTypingStats(MEMBER_ID, LANG);

    verify(yesterdayBatchStatusCache, never()).markDone(any(), any(), any());
    verify(memberWordTypingAggregationRepository)
        .aggregateByMemberIdsAndLanguageBetween(
            eq(List.of(MEMBER_ID)), eq(LANG), any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("refreshStats는 모든 언어의 완료 마커를 삭제한다")
  void refresh_clearsMarkers() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);

    service.refreshStats(MEMBER_ID, LANG);

    verify(yesterdayBatchStatusCache).clear(Mode.WORD, MEMBER_ID, WordLanguage.values());
  }
}
