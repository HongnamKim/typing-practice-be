package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class MemberTypingAggregation {
  private Long memberId;
  private int totalAttempts;
  private double avgCpm;
  private double avgAcc;
  private int bestCpm;
  private long totalPracticeTimeMin;
  private int totalResetCount;
  private LocalDateTime lastPracticedAt;
}
