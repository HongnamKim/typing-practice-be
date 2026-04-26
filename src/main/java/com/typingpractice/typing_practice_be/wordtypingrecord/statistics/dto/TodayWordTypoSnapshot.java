package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodayWordTypoSnapshot {
  private Map<String, Integer> typoCountMap;

  public static TodayWordTypoSnapshot create(Map<String, Integer> typoCountMap) {
    TodayWordTypoSnapshot snapshot = new TodayWordTypoSnapshot();
    snapshot.typoCountMap = new HashMap<>(typoCountMap);

    return snapshot;
  }

  public static TodayWordTypoSnapshot empty() {
    TodayWordTypoSnapshot snapshot = new TodayWordTypoSnapshot();
    snapshot.typoCountMap = new HashMap<>();
    return snapshot;
  }

  public void increment(WordLanguage language, String expected) {
    String key = language + ":" + expected;
    typoCountMap.merge(key, 1, Integer::sum);
  }

  public static String toKey(WordLanguage language, String expected) {
    return language + ":" + expected;
  }
}
