package com.typingpractice.typing_practice_be.typingrecord.dto;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoSnapshot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTypoStatsResponse {
  private List<TypoEntry> content;

  public static MemberTypoStatsResponse of(
      QuoteLanguage language,
      List<MemberTypoStats> pgList,
      List<MemberTypoAggregation> yesterdayList,
      TodayTypoSnapshot today) {

    Map<String, Integer> mergedMap = new HashMap<>();

    for (MemberTypoStats stats : pgList) {
      String key = TodayTypoSnapshot.toKey(stats.getLanguage(), stats.getExpected());
      mergedMap.put(key, stats.getTypoCount());
    }

    // 어제 MongoDB 데이터 (expected별 합산)
    for (MemberTypoAggregation agg : yesterdayList) {
      String key = TodayTypoSnapshot.toKey(agg.getLanguage(), agg.getExpected());
      mergedMap.merge(key, agg.getCount(), Integer::sum);
    }

    for (Map.Entry<String, Integer> entry : today.getTypoCountMap().entrySet()) {
      mergedMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
    }

    String prefix = language + ":";
    MemberTypoStatsResponse response = new MemberTypoStatsResponse();
    response.content =
        mergedMap.entrySet().stream()
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

  public static MemberTypoStatsResponse of(
      QuoteLanguage language, List<MemberTypoStats> pgList, TodayTypoSnapshot today) {
    Map<String, Integer> mergedMap = new HashMap<>();

    for (MemberTypoStats stats : pgList) {
      String key = TodayTypoSnapshot.toKey(stats.getLanguage(), stats.getExpected());
      mergedMap.put(key, stats.getTypoCount());
    }

    for (Map.Entry<String, Integer> entry : today.getTypoCountMap().entrySet()) {
      mergedMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
    }

    String prefix = language + ":";
    MemberTypoStatsResponse response = new MemberTypoStatsResponse();
    response.content =
        mergedMap.entrySet().stream()
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
    private QuoteLanguage language;
    private String expected;
    private int count;

    public static TypoEntry create(QuoteLanguage language, String expected, int count) {
      TypoEntry entry = new TypoEntry();
      entry.language = language;
      entry.expected = expected;
      entry.count = count;
      return entry;
    }
  }
}
