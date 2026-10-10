package com.typingpractice.typing_practice_be.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.service.YesterdayBatchStatusCache.Mode;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoDetailAllResponse;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberDailyAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypingAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypoAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoDetailStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailEntry;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberDailyStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoDetailStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.TodayTypingStatsRedisService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/** 히트맵용 전체 오타 상세 조회 (getAllTypoDetailStats) */
@ExtendWith(MockitoExtension.class)
class MemberQuoteStatisticsServiceTypoDetailAllTest {
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
  @Mock private YesterdayBatchStatusCache yesterdayBatchStatusCache;

  @InjectMocks private MemberQuoteStatisticsService service;

  @Test
  @DisplayName("어제 배치 전이면 어제 Mongo 집계 중 요청 언어만 PG·오늘 데이터와 합친다")
  void pendingYesterday_mergesOnlyRequestedLanguage() {
    when(memberTypoDetailStatsRepository.findByMemberIdAndLanguage(MEMBER_ID, LANG))
        .thenReturn(List.of(MemberTypoDetailStats.create(null, LANG, "ㄱ", "ㄴ", 3, 3, 0, 0, 0)));
    when(todayTypingStatsRedisService.getTypoDetailByLanguage(MEMBER_ID, LANG))
        .thenReturn(
            TodayTypoDetailSnapshot.create(
                Map.of("KOREAN: :", TodayTypoDetailEntry.create(4, 0, 0, 0, 4))));
    when(typingRecordRepository.existsByMemberIdBetween(
            eq(MEMBER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true);
    when(memberTypoAggregationRepository.aggregateByMemberIdsBetween(
            eq(List.of(MEMBER_ID)), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(
            List.of(
                aggregation(LANG, "ㄱ", "ㄴ", 2), aggregation(QuoteLanguage.ENGLISH, " ", "x", 9)));

    MemberTypoDetailAllResponse response = service.getAllTypoDetailStats(MEMBER_ID, LANG);

    assertThat(response.getContent().getLanguage()).isEqualTo(LANG);
    assertThat(response.getContent().getTypos())
        .extracting("expected", "actual", "typoCount")
        .containsExactly(tuple("ㄱ", "ㄴ", 5), tuple(" ", "", 4));
  }

  @Test
  @DisplayName("어제 배치가 끝났으면 Mongo 집계 없이 PG·오늘 데이터만 쓴다")
  void batchDone_skipsMongoAggregation() {
    when(yesterdayBatchStatusCache.isDone(Mode.QUOTE, MEMBER_ID, LANG)).thenReturn(true);
    when(memberTypoDetailStatsRepository.findByMemberIdAndLanguage(MEMBER_ID, LANG))
        .thenReturn(List.of(MemberTypoDetailStats.create(null, LANG, "ㄱ", "ㄴ", 3, 3, 0, 0, 0)));
    when(todayTypingStatsRedisService.getTypoDetailByLanguage(MEMBER_ID, LANG))
        .thenReturn(TodayTypoDetailSnapshot.empty());

    MemberTypoDetailAllResponse response = service.getAllTypoDetailStats(MEMBER_ID, LANG);

    assertThat(response.getContent().getTypos())
        .extracting("expected", "actual", "typoCount")
        .containsExactly(tuple("ㄱ", "ㄴ", 3));
    verifyNoInteractions(memberTypoAggregationRepository);
  }

  private static MemberTypoAggregation aggregation(
      QuoteLanguage language, String expected, String actual, int count) {
    MemberTypoAggregation aggregation = new MemberTypoAggregation();
    ReflectionTestUtils.setField(aggregation, "language", language);
    ReflectionTestUtils.setField(aggregation, "expected", expected);
    ReflectionTestUtils.setField(aggregation, "actual", actual);
    ReflectionTestUtils.setField(aggregation, "count", count);
    return aggregation;
  }
}
