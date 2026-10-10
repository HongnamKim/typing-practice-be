package com.typingpractice.typing_practice_be.wordtypingrecord.dto.response;

import com.typingpractice.typing_practice_be.common.utils.TypoDetailKeyUtils;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoDetailStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailEntry;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 키보드 히트맵용 전체 오타 상세 (정답·입력·횟수만) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWordTypoDetailAllResponse {
  private Content content;

  public static MemberWordTypoDetailAllResponse of(
      WordLanguage language,
      List<MemberWordTypoDetailStats> pgList,
      List<MemberWordTypoAggregation> yesterdayList,
      TodayWordTypoDetailSnapshot today) {
    Map<TypoKey, Integer> counts = new HashMap<>();

    for (MemberWordTypoDetailStats stats : pgList) {
      counts.merge(
          new TypoKey(stats.getExpected(), stats.getActual()), stats.getTypoCount(), Integer::sum);
    }

    for (MemberWordTypoAggregation agg : yesterdayList) {
      counts.merge(new TypoKey(agg.getExpected(), agg.getActual()), agg.getCount(), Integer::sum);
    }

    for (Map.Entry<String, TodayWordTypoDetailEntry> entry : today.getDetailMap().entrySet()) {
      String[] expectedAndActual =
          TypoDetailKeyUtils.splitExpectedAndActual(language, entry.getKey());
      counts.merge(
          new TypoKey(expectedAndActual[0], expectedAndActual[1]),
          entry.getValue().getCount(),
          Integer::sum);
    }

    MemberWordTypoDetailAllResponse response = new MemberWordTypoDetailAllResponse();
    response.content = new Content();
    response.content.language = language;
    response.content.typos =
        counts.entrySet().stream()
            .map(e -> TypoEntry.of(e.getKey().expected(), e.getKey().actual(), e.getValue()))
            .sorted((a, b) -> Integer.compare(b.getTypoCount(), a.getTypoCount()))
            .toList();
    return response;
  }

  private record TypoKey(String expected, String actual) {}

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class Content {
    private WordLanguage language;
    private List<TypoEntry> typos;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class TypoEntry {
    private String expected;
    private String actual;
    private int typoCount;

    private static TypoEntry of(String expected, String actual, int typoCount) {
      TypoEntry entry = new TypoEntry();
      entry.expected = expected;
      entry.actual = actual;
      entry.typoCount = typoCount;
      return entry;
    }
  }
}
