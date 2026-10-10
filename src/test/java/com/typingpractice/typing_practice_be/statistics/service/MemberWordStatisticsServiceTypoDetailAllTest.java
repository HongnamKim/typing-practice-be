package com.typingpractice.typing_practice_be.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.statistics.service.YesterdayBatchStatusCache.Mode;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.response.MemberWordTypoDetailAllResponse;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.WordTypingRecordRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberDailyWordAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypingAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypoAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoDetailStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailEntry;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberDailyWordStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypingStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypoDetailStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypoStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service.TodayWordTypingStatsRedisService;
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
class MemberWordStatisticsServiceTypoDetailAllTest {
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
  @Mock private YesterdayBatchStatusCache yesterdayBatchStatusCache;

  @InjectMocks private MemberWordStatisticsService service;

  @Test
  @DisplayName("어제 배치 전이면 어제 Mongo 집계 중 요청 언어만 PG·오늘 데이터와 합친다")
  void pendingYesterday_mergesOnlyRequestedLanguage() {
    when(memberWordTypoDetailStatsRepository.findByMemberIdAndLanguage(MEMBER_ID, LANG))
        .thenReturn(
            List.of(MemberWordTypoDetailStats.create(null, LANG, "ㄱ", "ㄴ", 3, 3, 0, 0, 0)));
    when(todayWordTypingStatsRedisService.getTypoDetailByLanguage(MEMBER_ID, LANG))
        .thenReturn(
            TodayWordTypoDetailSnapshot.create(
                Map.of("KOREAN: :", TodayWordTypoDetailEntry.create(4, 0, 0, 0, 4))));
    when(wordTypingRecordRepository.existsByMemberIdBetween(
            eq(MEMBER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true);
    when(memberWordTypoAggregationRepository.aggregateByMemberIdsBetween(
            eq(List.of(MEMBER_ID)), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(
            List.of(
                aggregation(LANG, "ㄱ", "ㄴ", 2), aggregation(WordLanguage.ENGLISH, " ", "x", 9)));

    MemberWordTypoDetailAllResponse response = service.getAllTypoDetailStats(MEMBER_ID, LANG);

    assertThat(response.getContent().getLanguage()).isEqualTo(LANG);
    assertThat(response.getContent().getTypos())
        .extracting("expected", "actual", "typoCount")
        .containsExactly(tuple("ㄱ", "ㄴ", 5), tuple(" ", "", 4));
  }

  @Test
  @DisplayName("어제 배치가 끝났으면 Mongo 집계 없이 PG·오늘 데이터만 쓴다")
  void batchDone_skipsMongoAggregation() {
    when(yesterdayBatchStatusCache.isDone(Mode.WORD, MEMBER_ID, LANG)).thenReturn(true);
    when(memberWordTypoDetailStatsRepository.findByMemberIdAndLanguage(MEMBER_ID, LANG))
        .thenReturn(
            List.of(MemberWordTypoDetailStats.create(null, LANG, "ㄱ", "ㄴ", 3, 3, 0, 0, 0)));
    when(todayWordTypingStatsRedisService.getTypoDetailByLanguage(MEMBER_ID, LANG))
        .thenReturn(TodayWordTypoDetailSnapshot.empty());

    MemberWordTypoDetailAllResponse response = service.getAllTypoDetailStats(MEMBER_ID, LANG);

    assertThat(response.getContent().getTypos())
        .extracting("expected", "actual", "typoCount")
        .containsExactly(tuple("ㄱ", "ㄴ", 3));
    verifyNoInteractions(memberWordTypoAggregationRepository);
  }

  private static MemberWordTypoAggregation aggregation(
      WordLanguage language, String expected, String actual, int count) {
    MemberWordTypoAggregation aggregation = new MemberWordTypoAggregation();
    ReflectionTestUtils.setField(aggregation, "language", language);
    ReflectionTestUtils.setField(aggregation, "expected", expected);
    ReflectionTestUtils.setField(aggregation, "actual", actual);
    ReflectionTestUtils.setField(aggregation, "count", count);
    return aggregation;
  }
}
