package com.typingpractice.typing_practice_be.statistics.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberDailyStatsRequest {
  @Min(value = 7, message = "days는 최소 7입니다.")
  @Max(value = 90, message = "days는 최대 90입니다.")
  private final int days;

  public MemberDailyStatsRequest(Integer days) {
    this.days = days != null ? days : 7;
  }
}
