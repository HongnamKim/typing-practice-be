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
  private float avgCpm;
  private float avgAcc;
  private int bestCpm;
  private int resetCount;
  private float practiceTimeMin;

  public LocalDate getDateAsLocalDate() {
    return LocalDate.parse(date);
  }

  public static MemberDailyAggregation create(
      Long memberId,
      String date,
      int attempts,
      float avgCpm,
      float avgAcc,
      int bestCpm,
      int resetCount,
      float practiceTimeMin) {
    MemberDailyAggregation agg = new MemberDailyAggregation();
    agg.memberId = memberId;
    agg.date = date;
    agg.attempts = attempts;
    agg.avgCpm = avgCpm;
    agg.avgAcc = avgAcc;
    agg.bestCpm = bestCpm;
    agg.resetCount = resetCount;
    agg.practiceTimeMin = practiceTimeMin;

    return agg;
  }
}
