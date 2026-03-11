package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class MemberTypingAggregation {
  private Long memberId;
  private int totalAttempts;
  private float avgCpm;
  private float avgAcc;
  private int bestCpm;
  private float totalPracticeTimeMin;
  private int totalResetCount;
  private LocalDateTime lastPracticedAt;

  public static MemberTypingAggregation create(
      Long memberId,
      int attempts,
      float avgCpm,
      float avgAcc,
      int bestCpm,
      int resetCount,
      float practiceTimeMin,
      LocalDateTime lastPracticedAt) {
    MemberTypingAggregation agg = new MemberTypingAggregation();
    agg.memberId = memberId;
    agg.totalAttempts = attempts;
    agg.avgCpm = avgCpm;
    agg.avgAcc = avgAcc;
    agg.bestCpm = bestCpm;
    agg.totalResetCount = resetCount;
    agg.totalPracticeTimeMin = practiceTimeMin;
    agg.lastPracticedAt = lastPracticedAt;

    return agg;
  }
}
