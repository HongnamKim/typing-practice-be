package com.typingpractice.typing_practice_be.typingrecord.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoDetailStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailEntry;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MemberTypoDetailAllResponseTest {
  private static final QuoteLanguage LANG = QuoteLanguage.KOREAN;

  @Test
  @DisplayName("PG, 어제 집계, 오늘 Redis 를 정답·입력 쌍 기준으로 합치고 횟수 내림차순으로 정렬한다")
  void mergesSourcesByExpectedAndActual() {
    List<MemberTypoDetailStats> pgList =
        List.of(
            MemberTypoDetailStats.create(null, LANG, "ㄱ", "ㄴ", 3, 3, 0, 0, 0),
            MemberTypoDetailStats.create(null, LANG, "ㅏ", "ㅓ", 1, 0, 1, 0, 0));
    List<MemberTypoAggregation> yesterdayList = List.of(aggregation("ㄱ", "ㄴ", 2));
    TodayTypoDetailSnapshot today =
        TodayTypoDetailSnapshot.create(
            Map.of(
                "KOREAN:ㄱ:ㄴ", TodayTypoDetailEntry.create(1, 1, 0, 0, 0),
                "KOREAN: :", TodayTypoDetailEntry.create(4, 0, 0, 0, 4)));

    MemberTypoDetailAllResponse response =
        MemberTypoDetailAllResponse.of(LANG, pgList, yesterdayList, today);

    assertThat(response.getContent().getLanguage()).isEqualTo(LANG);
    assertThat(response.getContent().getTypos())
        .extracting("expected", "actual", "typoCount")
        .containsExactly(tuple("ㄱ", "ㄴ", 6), tuple(" ", "", 4), tuple("ㅏ", "ㅓ", 1));
  }

  @Test
  @DisplayName("오늘 Redis 키에 콜론이 들어간 오타도 정답과 입력을 올바르게 나눈다")
  void splitsTodayKeysContainingColons() {
    TodayTypoDetailSnapshot today =
        TodayTypoDetailSnapshot.create(
            Map.of(
                "KOREAN:::ㄱ", TodayTypoDetailEntry.create(4, 0, 0, 0, 4), // ':' → 'ㄱ'
                "KOREAN:ㄱ::", TodayTypoDetailEntry.create(3, 3, 0, 0, 0), // 'ㄱ' → ':'
                "KOREAN::::", TodayTypoDetailEntry.create(2, 0, 0, 0, 2), // ':' → ':'
                "KOREAN::ㄱ", TodayTypoDetailEntry.create(1, 0, 0, 0, 1))); // '' → 'ㄱ'

    MemberTypoDetailAllResponse response =
        MemberTypoDetailAllResponse.of(LANG, List.of(), List.of(), today);

    assertThat(response.getContent().getTypos())
        .extracting("expected", "actual", "typoCount")
        .containsExactly(
            tuple(":", "ㄱ", 4), tuple("ㄱ", ":", 3), tuple(":", ":", 2), tuple("", "ㄱ", 1));
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
