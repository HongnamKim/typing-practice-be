package com.typingpractice.typing_practice_be.wordtypingrecord.dto.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoDetailStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailEntry;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWordTypoDetailStatsResponse {
  private List<DetailEntry> content;

  public static MemberWordTypoDetailStatsResponse of(
      WordLanguage language,
      String expected,
      List<MemberWordTypoDetailStats> pgList,
      List<MemberWordTypoAggregation> yesterdayList,
      TodayWordTypoDetailSnapshot todayFiltered) {
    Map<String, DetailEntry> mergedMap = new HashMap<>();

    for (MemberWordTypoDetailStats stats : pgList) {
      String key =
          TodayWordTypoDetailSnapshot.toKey(
              stats.getLanguage(), stats.getExpected(), stats.getActual());
      mergedMap.put(key, DetailEntry.from(stats));
    }

    for (MemberWordTypoAggregation agg : yesterdayList) {
      String key =
          TodayWordTypoDetailSnapshot.toKey(agg.getLanguage(), agg.getExpected(), agg.getActual());
      mergedMap.merge(key, DetailEntry.from(agg), DetailEntry::merge);
    }

    for (Map.Entry<String, TodayWordTypoDetailEntry> entry :
        todayFiltered.getDetailMap().entrySet()) {
      mergedMap.merge(
          entry.getKey(),
          DetailEntry.fromToday(language, expected, entry.getKey(), entry.getValue()),
          DetailEntry::merge);
    }

    MemberWordTypoDetailStatsResponse response = new MemberWordTypoDetailStatsResponse();
    response.content =
        mergedMap.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTypoCount(), a.getTypoCount()))
            .toList();

    return response;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class DetailEntry {
    private WordLanguage language;
    private String expected;
    private String actual;
    private int typoCount;
    private int initialCount;
    private int medialCount;
    private int finalCount;
    private int letterCount;

    public static DetailEntry from(MemberWordTypoAggregation agg) {
      DetailEntry entry = new DetailEntry();

      entry.language = agg.getLanguage();
      entry.expected = agg.getExpected();
      entry.actual = agg.getActual();
      entry.typoCount = agg.getCount();
      entry.initialCount = agg.getInitialCount();
      entry.medialCount = agg.getMedialCount();
      entry.finalCount = agg.getFinalCount();
      entry.letterCount = agg.getLetterCount();
      return entry;
    }

    public static DetailEntry from(MemberWordTypoDetailStats stats) {
      DetailEntry entry = new DetailEntry();
      entry.language = stats.getLanguage();
      entry.expected = stats.getExpected();
      entry.actual = stats.getActual();
      entry.typoCount = stats.getTypoCount();
      entry.initialCount = stats.getInitialCount();
      entry.medialCount = stats.getMedialCount();
      entry.finalCount = stats.getFinalCount();
      entry.letterCount = stats.getLetterCount();
      return entry;
    }

    public static DetailEntry fromToday(
        WordLanguage language, String expected, String hashKey, TodayWordTypoDetailEntry today) {
      String prefix = language + ":" + expected + ":";
      String actual = hashKey.substring(prefix.length());

      DetailEntry entry = new DetailEntry();
      entry.language = language;
      entry.expected = expected;
      entry.actual = actual;
      entry.typoCount = today.getCount();
      entry.initialCount = today.getInitialCount();
      entry.medialCount = today.getMedialCount();
      entry.finalCount = today.getFinalCount();
      entry.letterCount = today.getLetterCount();
      return entry;
    }

    public static DetailEntry merge(DetailEntry existing, DetailEntry today) {
      DetailEntry merged = new DetailEntry();
      merged.language = existing.language;
      merged.expected = existing.expected;
      merged.actual = existing.actual;
      merged.typoCount = existing.typoCount + today.typoCount;
      merged.initialCount = existing.initialCount + today.initialCount;
      merged.medialCount = existing.medialCount + today.medialCount;
      merged.finalCount = existing.finalCount + today.finalCount;
      merged.letterCount = existing.letterCount + today.letterCount;
      return merged;
    }
  }
}
