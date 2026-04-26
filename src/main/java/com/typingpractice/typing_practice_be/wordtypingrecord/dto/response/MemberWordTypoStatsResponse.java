package com.typingpractice.typing_practice_be.wordtypingrecord.dto.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWordTypoStatsResponse {
  private List<TypoEntry> content;

  public static MemberWordTypoStatsResponse of(
      WordLanguage language,
      List<MemberWordTypoStats> pgList,
      List<MemberWordTypoAggregation> yesterdayList,
      TodayWordTypoSnapshot today) {
    Map<String, Integer> mergedMap = new HashMap<>();

    for (MemberWordTypoStats stats : pgList) {
      String key = TodayWordTypoSnapshot.toKey(stats.getLanguage(), stats.getExpected());
      mergedMap.put(key, stats.getTypoCount());
    }

    for (MemberWordTypoAggregation agg : yesterdayList) {
      String key = TodayWordTypoSnapshot.toKey(agg.getLanguage(), agg.getExpected());
      mergedMap.merge(key, agg.getCount(), Integer::sum);
    }

    for (Map.Entry<String, Integer> entry : today.getTypoCountMap().entrySet()) {
      mergedMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
    }

    String prefix = language + ":";
    MemberWordTypoStatsResponse response = new MemberWordTypoStatsResponse();
    response.content =
        mergedMap.entrySet().stream()
            .filter(e -> e.getKey().startsWith(prefix))
            .map(
                e -> {
                  String expected = e.getKey().substring(prefix.length());
                  return TypoEntry.create(language, expected, e.getValue());
                })
            .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
            .limit(10)
            .toList();

    return response;
  }

  @Getter
  @ToString
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class TypoEntry {
    private WordLanguage language;
    private String expected;
    private int count;

    public static TypoEntry create(WordLanguage language, String expected, int count) {
      TypoEntry entry = new TypoEntry();
      entry.language = language;
      entry.expected = expected;
      entry.count = count;
      return entry;
    }
  }
}
