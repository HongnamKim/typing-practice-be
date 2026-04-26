package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto;

import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypoType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodayWordTypoDetailEntry {
  private int count;
  private int initialCount;
  private int medialCount;
  private int finalCount;
  private int letterCount;

  public static TodayWordTypoDetailEntry create(
      int count, int initialCount, int medialCount, int finalCount, int letterCount) {
    TodayWordTypoDetailEntry entry = new TodayWordTypoDetailEntry();
    entry.count = count;
    entry.initialCount = initialCount;
    entry.medialCount = medialCount;
    entry.finalCount = finalCount;
    entry.letterCount = letterCount;
    return entry;
  }

  public static TodayWordTypoDetailEntry empty() {
    return new TodayWordTypoDetailEntry();
  }

  public void increment(WordTypoType type) {
    this.count++;

    switch (type) {
      case INITIAL -> this.initialCount++;
      case MEDIAL -> this.medialCount++;
      case FINAL -> this.finalCount++;
      case LETTER -> this.letterCount++;
    }
  }
}
