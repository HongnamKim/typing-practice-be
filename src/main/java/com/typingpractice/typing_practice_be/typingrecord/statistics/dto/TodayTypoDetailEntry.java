package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import com.typingpractice.typing_practice_be.typingrecord.domain.TypoType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodayTypoDetailEntry {
  private int count;
  private int initialCount;
  private int medialCount;
  private int finalCount;
  private int letterCount;

  public static TodayTypoDetailEntry create(
      int count, int initialCount, int medialCount, int finalCount, int letterCount) {
    TodayTypoDetailEntry entry = new TodayTypoDetailEntry();
    entry.count = count;
    entry.initialCount = initialCount;
    entry.medialCount = medialCount;
    entry.finalCount = finalCount;
    entry.letterCount = letterCount;

    return entry;
  }

  public static TodayTypoDetailEntry empty() {
    return new TodayTypoDetailEntry();
  }

  public void increment(TypoType type) {
    this.count++;
    switch (type) {
      case INITIAL -> this.initialCount++;
      case MEDIAL -> this.medialCount++;
      case FINAL -> this.finalCount++;
      case LETTER -> this.letterCount++;
    }
  }
}
