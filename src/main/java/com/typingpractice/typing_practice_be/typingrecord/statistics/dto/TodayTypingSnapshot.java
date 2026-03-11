package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodayTypingSnapshot {
  private int totalAttempts;
  private float avgCpm;
  private float avgAcc;
  private int bestCpm;
  private float totalPracticeTimeMin;
  private int totalResetCount;

  public static TodayTypingSnapshot create(
      int totalAttempts,
      float avgCpm,
      float avgAcc,
      int bestCpm,
      float totalPracticeTimeMin,
      int totalResetCount) {
    TodayTypingSnapshot snapshot = new TodayTypingSnapshot();
    snapshot.totalAttempts = totalAttempts;
    snapshot.avgCpm = avgCpm;
    snapshot.avgAcc = avgAcc;
    snapshot.bestCpm = bestCpm;
    snapshot.totalPracticeTimeMin = totalPracticeTimeMin;
    snapshot.totalResetCount = totalResetCount;
    return snapshot;
  }

  public static TodayTypingSnapshot empty() {
    return new TodayTypingSnapshot();
  }

  public void increment(int cpm, float accuracy, int charLength, int resetCount) {
    int newTotal = this.totalAttempts + 1;
    this.avgCpm = (this.avgCpm * this.totalAttempts + cpm) / newTotal;
    this.avgAcc = (this.avgAcc * this.totalAttempts + accuracy) / newTotal;
    this.totalAttempts = newTotal;
    this.bestCpm = Math.max(this.bestCpm, cpm);
    if (cpm > 0) {
      this.totalPracticeTimeMin += (float) charLength / cpm;
    }
    this.totalResetCount += resetCount;
  }
}
