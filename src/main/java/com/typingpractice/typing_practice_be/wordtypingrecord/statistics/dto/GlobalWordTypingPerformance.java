package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GlobalWordTypingPerformance {
  private float avgWpm;
  private float avgAcc;
  private boolean empty;

  public static GlobalWordTypingPerformance of(float avgWpm, float avgAcc) {
    GlobalWordTypingPerformance perf = new GlobalWordTypingPerformance();
    perf.avgWpm = avgWpm;
    perf.avgAcc = avgAcc;
    perf.empty = false;
    return perf;
  }

  public static GlobalWordTypingPerformance empty() {
    GlobalWordTypingPerformance perf = new GlobalWordTypingPerformance();
    perf.empty = true;
    return perf;
  }
}
