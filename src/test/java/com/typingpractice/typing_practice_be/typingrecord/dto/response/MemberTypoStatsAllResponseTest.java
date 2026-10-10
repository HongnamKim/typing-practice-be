package com.typingpractice.typing_practice_be.typingrecord.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MemberTypoStatsAllResponseTest {
  private static final QuoteLanguage LANG = QuoteLanguage.KOREAN;

  @Test
  @DisplayName("PG, 어제 집계, 오늘 Redis 를 정답 글자별로 합치고 횟수 내림차순으로 정렬한다")
  void mergesSourcesByExpected() {
    List<MemberTypoStats> pgList =
        List.of(
            MemberTypoStats.create(null, LANG, "ㄱ", 3), MemberTypoStats.create(null, LANG, "ㅏ", 1));
    List<MemberTypoAggregation> yesterdayList =
        List.of(aggregation("ㄱ", "ㄴ", 2), aggregation("ㄱ", "ㄷ", 1));
    TodayTypoSnapshot today = TodayTypoSnapshot.create(Map.of("KOREAN:ㄱ", 1, "KOREAN: ", 4));

    MemberTypoStatsAllResponse response =
        MemberTypoStatsAllResponse.of(LANG, pgList, yesterdayList, today);

    assertThat(response.getContent().getLanguage()).isEqualTo(LANG);
    assertThat(response.getContent().getTypos())
        .extracting("expected", "count")
        .containsExactly(tuple("ㄱ", 7), tuple(" ", 4), tuple("ㅏ", 1));
  }

  private static MemberTypoAggregation aggregation(String expected, String actual, int count) {
    MemberTypoAggregation aggregation = new MemberTypoAggregation();
    ReflectionTestUtils.setField(aggregation, "language", LANG);
    ReflectionTestUtils.setField(aggregation, "expected", expected);
    ReflectionTestUtils.setField(aggregation, "actual", actual);
    ReflectionTestUtils.setField(aggregation, "count", count);
    return aggregation;
  }
}
