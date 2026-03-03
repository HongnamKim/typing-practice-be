package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class MemberDailyAggregation {
  private Long memberId;
  private String date; // MongoDB에서 "%Y-%m-%d" 포맷으로 수신
  private int attempts;
  private double avgCpm;
  private double avgAcc;
  private int bestCpm;
  private int resetCount;
  private float practiceTimeMin;

  public LocalDate getDateAsLocalDate() {
    return LocalDate.parse(date);
  }
}
