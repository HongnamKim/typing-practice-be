package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodayTypoDetailSnapshot {
  private Map<String, TodayTypoDetailEntry> detailMap;

  public static TodayTypoDetailSnapshot create(Map<String, TodayTypoDetailEntry> detailMap) {
    TodayTypoDetailSnapshot snapshot = new TodayTypoDetailSnapshot();
    snapshot.detailMap = new HashMap<>(detailMap);
    return snapshot;
  }

  public static TodayTypoDetailSnapshot empty() {
    TodayTypoDetailSnapshot snapshot = new TodayTypoDetailSnapshot();
    snapshot.detailMap = new HashMap<>();
    return snapshot;
  }

  public static String toKey(QuoteLanguage language, String expected, String actual) {
    return language + ":" + expected + ":" + actual;
  }
}
