package com.typingpractice.typing_practice_be.wordtypingrecord.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MemberWordTypoStatsAllResponseTest {
  private static final WordLanguage LANG = WordLanguage.KOREAN;

  @Test
  @DisplayName("PG, 어제 집계, 오늘 Redis 를 정답 글자별로 합치고 횟수 내림차순으로 정렬한다")
  void mergesSourcesByExpected() {
    List<MemberWordTypoStats> pgList =
        List.of(
            MemberWordTypoStats.create(null, LANG, "ㄱ", 3),
            MemberWordTypoStats.create(null, LANG, "ㅏ", 1));
    List<MemberWordTypoAggregation> yesterdayList =
        List.of(aggregation("ㄱ", "ㄴ", 2), aggregation("ㄱ", "ㄷ", 1));
    TodayWordTypoSnapshot today =
        TodayWordTypoSnapshot.create(Map.of("KOREAN:ㄱ", 1, "KOREAN: ", 4));

    MemberWordTypoStatsAllResponse response =
        MemberWordTypoStatsAllResponse.of(LANG, pgList, yesterdayList, today);

    assertThat(response.getContent().getLanguage()).isEqualTo(LANG);
    assertThat(response.getContent().getTypos())
        .extracting("expected", "count")
        .containsExactly(tuple("ㄱ", 7), tuple(" ", 4), tuple("ㅏ", 1));
  }

  private static MemberWordTypoAggregation aggregation(
      String expected, String actual, int count) {
    MemberWordTypoAggregation aggregation = new MemberWordTypoAggregation();
    ReflectionTestUtils.setField(aggregation, "language", LANG);
    ReflectionTestUtils.setField(aggregation, "expected", expected);
    ReflectionTestUtils.setField(aggregation, "actual", actual);
    ReflectionTestUtils.setField(aggregation, "count", count);
    return aggregation;
  }
}
