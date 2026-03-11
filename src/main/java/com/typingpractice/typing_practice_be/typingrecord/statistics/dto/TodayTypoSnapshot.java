package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodayTypoSnapshot {
  private Map<String, Integer> typoCountMap;

  public static TodayTypoSnapshot create(Map<String, Integer> typoCountMap) {
    TodayTypoSnapshot snapshot = new TodayTypoSnapshot();
    snapshot.typoCountMap = new HashMap<>(typoCountMap);
    return snapshot;
  }

  public static TodayTypoSnapshot empty() {
    TodayTypoSnapshot snapshot = new TodayTypoSnapshot();
    snapshot.typoCountMap = new HashMap<>();
    return snapshot;
  }

  public void increment(QuoteLanguage language, String expected) {
    String key = language + ":" + expected;
    typoCountMap.merge(key, 1, Integer::sum);
  }

  public static String toKey(QuoteLanguage language, String expected) {
    return language + ":" + expected;
  }
}
