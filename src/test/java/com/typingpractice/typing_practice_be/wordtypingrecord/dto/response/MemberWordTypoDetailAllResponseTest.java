package com.typingpractice.typing_practice_be.wordtypingrecord.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoDetailStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailEntry;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MemberWordTypoDetailAllResponseTest {
  private static final WordLanguage LANG = WordLanguage.KOREAN;

  @Test
  @DisplayName("PG, 어제 집계, 오늘 Redis 를 정답·입력 쌍 기준으로 합치고 횟수 내림차순으로 정렬한다")
  void mergesSourcesByExpectedAndActual() {
    List<MemberWordTypoDetailStats> pgList =
        List.of(
            MemberWordTypoDetailStats.create(null, LANG, "ㄱ", "ㄴ", 3, 3, 0, 0, 0),
            MemberWordTypoDetailStats.create(null, LANG, "ㅏ", "ㅓ", 1, 0, 1, 0, 0));
    List<MemberWordTypoAggregation> yesterdayList = List.of(aggregation("ㄱ", "ㄴ", 2));
    TodayWordTypoDetailSnapshot today =
        TodayWordTypoDetailSnapshot.create(
            Map.of(
                "KOREAN:ㄱ:ㄴ", TodayWordTypoDetailEntry.create(1, 1, 0, 0, 0),
                "KOREAN: :", TodayWordTypoDetailEntry.create(4, 0, 0, 0, 4)));

    MemberWordTypoDetailAllResponse response =
        MemberWordTypoDetailAllResponse.of(LANG, pgList, yesterdayList, today);

    assertThat(response.getContent().getLanguage()).isEqualTo(LANG);
    assertThat(response.getContent().getTypos())
        .extracting("expected", "actual", "typoCount")
        .containsExactly(tuple("ㄱ", "ㄴ", 6), tuple(" ", "", 4), tuple("ㅏ", "ㅓ", 1));
  }

  @Test
  @DisplayName("오늘 Redis 키에 콜론이 들어간 오타도 정답과 입력을 올바르게 나눈다")
  void splitsTodayKeysContainingColons() {
    TodayWordTypoDetailSnapshot today =
        TodayWordTypoDetailSnapshot.create(
            Map.of(
                "KOREAN:::ㄱ", TodayWordTypoDetailEntry.create(4, 0, 0, 0, 4), // ':' → 'ㄱ'
                "KOREAN:ㄱ::", TodayWordTypoDetailEntry.create(3, 3, 0, 0, 0), // 'ㄱ' → ':'
                "KOREAN::::", TodayWordTypoDetailEntry.create(2, 0, 0, 0, 2), // ':' → ':'
                "KOREAN::ㄱ", TodayWordTypoDetailEntry.create(1, 0, 0, 0, 1))); // '' → 'ㄱ'

    MemberWordTypoDetailAllResponse response =
        MemberWordTypoDetailAllResponse.of(LANG, List.of(), List.of(), today);

    assertThat(response.getContent().getTypos())
        .extracting("expected", "actual", "typoCount")
        .containsExactly(
            tuple(":", "ㄱ", 4), tuple("ㄱ", ":", 3), tuple(":", ":", 2), tuple("", "ㄱ", 1));
  }

  private static MemberWordTypoAggregation aggregation(String expected, String actual, int count) {
    MemberWordTypoAggregation aggregation = new MemberWordTypoAggregation();
    ReflectionTestUtils.setField(aggregation, "language", LANG);
    ReflectionTestUtils.setField(aggregation, "expected", expected);
    ReflectionTestUtils.setField(aggregation, "actual", actual);
    ReflectionTestUtils.setField(aggregation, "count", count);
    return aggregation;
  }
}
