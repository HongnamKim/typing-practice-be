package com.typingpractice.typing_practice_be.statistics.dto;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@ToString
public class MemberStatsPeriodRequest {
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private final LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private final LocalDate endDate;

  private final String timezone;

  public MemberStatsPeriodRequest(LocalDate startDate, LocalDate endDate, String timezone) {
    this.startDate = startDate;
    this.endDate = endDate;
    this.timezone = timezone != null ? timezone : TimeUtils.KST_ZONE;
  }

  @AssertTrue(message = "startDate는 KST 기준 어제 이하여야 합니다.")
  private boolean isStartDateValid() {
    return startDate == null || !startDate.isAfter(LocalDate.now(TimeUtils.KST).minusDays(1));
  }

  @AssertTrue(message = "endDate는 KST 기준 어제 이하여야 합니다.")
  private boolean isEndDateValid() {
    return endDate == null || !endDate.isAfter(LocalDate.now(TimeUtils.KST).minusDays(1));
  }

  @AssertTrue(message = "startDate는 endDate보다 같거나 이전이어야 합니다.")
  private boolean isDateRangeValid() {
    return startDate == null || endDate == null || !startDate.isAfter(endDate);
  }
}
