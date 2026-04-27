package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodayWordTypoDetailSnapshot {
  private Map<String, TodayWordTypoDetailEntry> detailMap;

  public static TodayWordTypoDetailSnapshot create(
      Map<String, TodayWordTypoDetailEntry> detailMap) {
    TodayWordTypoDetailSnapshot snapshot = new TodayWordTypoDetailSnapshot();
    snapshot.detailMap = new HashMap<>(detailMap);

    return snapshot;
  }

  public static TodayWordTypoDetailSnapshot empty() {
    TodayWordTypoDetailSnapshot snapshot = new TodayWordTypoDetailSnapshot();
    snapshot.detailMap = new HashMap<>();
    return snapshot;
  }

  public static String toKey(WordLanguage language, String expected, String actual) {
    return language + ":" + expected + ":" + actual;
  }
}
