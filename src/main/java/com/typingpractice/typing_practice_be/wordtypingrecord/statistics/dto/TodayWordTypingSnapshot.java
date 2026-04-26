package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodayWordTypingSnapshot {
  private int totalAttempts;
  private int totalWordsAttempted;
  private float avgWpm;
  private float avgAcc;
  private float bestWpm;
  private float totalPracticeTimeMin;

  public static TodayWordTypingSnapshot create(
      int totalAttempts,
      int totalWordsAttempted,
      float avgWpm,
      float avgAcc,
      float bestWpm,
      float totalPracticeTimeMin) {
    TodayWordTypingSnapshot snapshot = new TodayWordTypingSnapshot();
    snapshot.totalAttempts = totalAttempts;
    snapshot.totalWordsAttempted = totalWordsAttempted;
    snapshot.avgWpm = avgWpm;
    snapshot.avgAcc = avgAcc;
    snapshot.bestWpm = bestWpm;
    snapshot.totalPracticeTimeMin = totalPracticeTimeMin;
    return snapshot;
  }

  public static TodayWordTypingSnapshot empty() {
    return new TodayWordTypingSnapshot();
  }

  public void increment(float wpm, float accuracy, int wordCount, long elapsedTimeMs) {
    int newTotal = this.totalAttempts + 1;
    this.avgWpm = (this.avgWpm * this.totalAttempts + wpm) / newTotal;
    this.avgAcc = (this.avgAcc * this.totalAttempts + accuracy) / newTotal;
    this.totalAttempts = newTotal;
    this.totalWordsAttempted += wordCount;
    this.bestWpm = Math.max(this.bestWpm, wpm);
    this.totalPracticeTimeMin += (float) elapsedTimeMs / 60000f;
  }
}
