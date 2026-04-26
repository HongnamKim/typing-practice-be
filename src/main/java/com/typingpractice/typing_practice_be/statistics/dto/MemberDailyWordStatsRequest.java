package com.typingpractice.typing_practice_be.statistics.dto;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberDailyWordStatsRequest {
  @Min(value = 7, message = "days는 최소 7입니다.")
  @Max(value = 90, message = "days는 최대 90입니다.")
  private final int days;

  @NotNull private final WordLanguage language;

  public MemberDailyWordStatsRequest(Integer days, WordLanguage language) {
    this.days = days != null ? days : 7;
    this.language = language;
  }
}
