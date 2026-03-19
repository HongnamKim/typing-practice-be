package com.typingpractice.typing_practice_be.typingrecord.dto;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoDetailStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailEntry;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoDetailSnapshot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTypoDetailStatsResponse {
  private List<DetailEntry> content;

  public static MemberTypoDetailStatsResponse of(
      QuoteLanguage language,
      String expected,
      List<MemberTypoDetailStats> pgList,
      TodayTypoDetailSnapshot todayFiltered) {

    // language:expected:actual → DetailEntry
    Map<String, DetailEntry> mergedMap = new HashMap<>();

    for (MemberTypoDetailStats stats : pgList) {
      String key =
          TodayTypoDetailSnapshot.toKey(
              stats.getLanguage(), stats.getExpected(), stats.getActual());
      mergedMap.put(key, DetailEntry.from(stats));
    }

    for (Map.Entry<String, TodayTypoDetailEntry> entry : todayFiltered.getDetailMap().entrySet()) {
      mergedMap.merge(
          entry.getKey(),
          DetailEntry.fromToday(language, expected, entry.getKey(), entry.getValue()),
          DetailEntry::merge);
    }

    MemberTypoDetailStatsResponse response = new MemberTypoDetailStatsResponse();
    response.content =
        mergedMap.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTypoCount(), a.getTypoCount()))
            .toList();

    return response;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class DetailEntry {
    private QuoteLanguage language;
    private String expected;
    private String actual;
    private int typoCount;
    private int initialCount;
    private int medialCount;
    private int finalCount;
    private int letterCount;

    public static DetailEntry from(MemberTypoDetailStats stats) {
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
        QuoteLanguage language, String expected, String hashKey, TodayTypoDetailEntry today) {
      // hashKey: "{language}:{expected}:{actual}"
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
