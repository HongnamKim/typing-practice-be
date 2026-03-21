package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GlobalTypingPerformance {
  private Float avgCpm;
  private Float avgAcc;

  public static GlobalTypingPerformance of(Float avgCpm, Float avgAcc) {
    GlobalTypingPerformance perf = new GlobalTypingPerformance();
    perf.avgCpm = avgCpm;
    perf.avgAcc = avgAcc;

    return perf;
  }

  public static GlobalTypingPerformance empty() {
    return new GlobalTypingPerformance();
  }

  public boolean isEmpty() {
    return avgCpm == null || avgAcc == null;
  }
}
