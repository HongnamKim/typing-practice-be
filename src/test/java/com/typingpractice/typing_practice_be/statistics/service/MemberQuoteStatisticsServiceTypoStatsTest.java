package com.typingpractice.typing_practice_be.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.service.YesterdayBatchStatusCache.Mode;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoDetailStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoStatsAllResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberDailyAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypingAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypoAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberDailyStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoDetailStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.TodayTypingStatsRedisService;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

/** 글자별 오타 통계 조회 (getAllTypoStats, getTypoStats, getTypoDetailStats) */
@ExtendWith(MockitoExtension.class)
class MemberQuoteStatisticsServiceTypoStatsTest {
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
  @DisplayName("전체 오타: 어제 배치 전이면 어제 Mongo 집계 중 요청 언어만 PG·오늘 데이터와 합친다")
  void getAllTypoStats_pendingYesterday_mergesOnlyRequestedLanguage() {
    when(memberTypoStatsRepository.findByMemberIdAndLanguage(MEMBER_ID, LANG))
        .thenReturn(List.of(MemberTypoStats.create(null, LANG, "ㄱ", 3)));
    when(todayTypingStatsRedisService.getTypoByLanguage(MEMBER_ID, LANG))
        .thenReturn(TodayTypoSnapshot.create(Map.of("KOREAN: ", 4)));
    stubYesterdayPending(
        aggregation(LANG, "ㄱ", "ㄴ", 2), aggregation(QuoteLanguage.ENGLISH, " ", "x", 9));

    MemberTypoStatsAllResponse response = service.getAllTypoStats(MEMBER_ID, LANG);

    assertThat(response.getContent().getLanguage()).isEqualTo(LANG);
    assertThat(response.getContent().getTypos())
        .extracting("expected", "count")
        .containsExactly(tuple("ㄱ", 5), tuple(" ", 4));
  }

  @Test
  @DisplayName("전체 오타: 어제 배치가 끝났으면 Mongo 집계 없이 PG·오늘 데이터만 쓴다")
  void getAllTypoStats_batchDone_skipsMongoAggregation() {
    when(yesterdayBatchStatusCache.isDone(Mode.QUOTE, MEMBER_ID, LANG)).thenReturn(true);
    when(memberTypoStatsRepository.findByMemberIdAndLanguage(MEMBER_ID, LANG))
        .thenReturn(List.of(MemberTypoStats.create(null, LANG, "ㄱ", 3)));
    when(todayTypingStatsRedisService.getTypoByLanguage(MEMBER_ID, LANG))
        .thenReturn(TodayTypoSnapshot.empty());

    MemberTypoStatsAllResponse response = service.getAllTypoStats(MEMBER_ID, LANG);

    assertThat(response.getContent().getTypos())
        .extracting("expected", "count")
        .containsExactly(tuple("ㄱ", 3));
    verifyNoInteractions(memberTypoAggregationRepository);
  }

  @Test
  @DisplayName("상위 10개: PG 전체에 오늘 데이터를 더한 뒤 10개를 고르고, 다른 언어의 어제 집계는 섞지 않는다")
  void getTypoStats_mergesAllBeforeLimitAndSkipsOtherLanguages() {
    // PG 11위 ㅋ(1회)는 오늘 9회를 더하면 1위가 된다
    List<MemberTypoStats> pgList = new ArrayList<>();
    for (String expected : List.of("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ")) {
      pgList.add(MemberTypoStats.create(null, LANG, expected, 2));
    }
    pgList.add(MemberTypoStats.create(null, LANG, "ㅋ", 1));
    when(memberTypoStatsRepository.findByMemberIdAndLanguage(MEMBER_ID, LANG)).thenReturn(pgList);
    when(todayTypingStatsRedisService.getTypoByLanguage(MEMBER_ID, LANG))
        .thenReturn(TodayTypoSnapshot.create(Map.of("KOREAN:ㅋ", 9)));
    stubYesterdayPending(aggregation(QuoteLanguage.ENGLISH, "a", "s", 30));

    MemberTypoStatsResponse response = service.getTypoStats(MEMBER_ID, LANG);

    assertThat(response.getContent()).hasSize(10);
    assertThat(response.getContent().get(0).getExpected()).isEqualTo("ㅋ");
    assertThat(response.getContent().get(0).getCount()).isEqualTo(10);
    assertThat(response.getContent()).extracting("expected").doesNotContain("a", ":a");
  }

  @Test
  @DisplayName("글자 상세: 어제 Mongo 집계 중 요청 언어만 합친다")
  void getTypoDetailStats_skipsOtherLanguagesFromYesterday() {
    when(memberTypoDetailStatsRepository.findByMemberIdAndLanguageAndExpected(
            MEMBER_ID, LANG, " "))
        .thenReturn(List.of());
    when(todayTypingStatsRedisService.getTypoDetailByLanguageAndExpected(MEMBER_ID, LANG, " "))
        .thenReturn(TodayTypoDetailSnapshot.empty());
    stubYesterdayPending(
        aggregation(LANG, " ", "", 2), aggregation(QuoteLanguage.ENGLISH, " ", "", 9));

    MemberTypoDetailStatsResponse response = service.getTypoDetailStats(MEMBER_ID, LANG, " ");

    assertThat(response.getContent())
        .extracting("language", "expected", "actual", "typoCount")
        .containsExactly(tuple(LANG, " ", "", 2));
  }

  private void stubYesterdayPending(MemberTypoAggregation... aggregations) {
    when(typingRecordRepository.existsByMemberIdBetween(
            eq(MEMBER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(true);
    when(memberTypoAggregationRepository.aggregateByMemberIdsBetween(
            eq(List.of(MEMBER_ID)), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(List.of(aggregations));
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
