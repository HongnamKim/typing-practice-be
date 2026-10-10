package com.typingpractice.typing_practice_be.typingrecord.dto.response;

import com.typingpractice.typing_practice_be.common.utils.TypoDetailKeyUtils;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoDetailStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailEntry;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 키보드 히트맵용 전체 오타 상세 (정답·입력·횟수만) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTypoDetailAllResponse {
  private Content content;

  public static MemberTypoDetailAllResponse of(
      QuoteLanguage language,
      List<MemberTypoDetailStats> pgList,
      List<MemberTypoAggregation> yesterdayList,
      TodayTypoDetailSnapshot today) {
    Map<TypoKey, Integer> counts = new HashMap<>();

    for (MemberTypoDetailStats stats : pgList) {
      counts.merge(
          new TypoKey(stats.getExpected(), stats.getActual()), stats.getTypoCount(), Integer::sum);
    }

    for (MemberTypoAggregation agg : yesterdayList) {
      counts.merge(new TypoKey(agg.getExpected(), agg.getActual()), agg.getCount(), Integer::sum);
    }

    for (Map.Entry<String, TodayTypoDetailEntry> entry : today.getDetailMap().entrySet()) {
      String[] expectedAndActual =
          TypoDetailKeyUtils.splitExpectedAndActual(language, entry.getKey());
      counts.merge(
          new TypoKey(expectedAndActual[0], expectedAndActual[1]),
          entry.getValue().getCount(),
          Integer::sum);
    }

    MemberTypoDetailAllResponse response = new MemberTypoDetailAllResponse();
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
    private QuoteLanguage language;
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
