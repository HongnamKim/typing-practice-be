package com.typingpractice.typing_practice_be.statistics.dto;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@ToString
public class MemberStatsDayRequest {
  @NotNull(message = "date는 필수입니다.")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private final LocalDate date;

  private final String timezone;

  public MemberStatsDayRequest(LocalDate date, String timezone) {
    this.date = date;
    this.timezone = timezone != null ? timezone : TimeUtils.KST_ZONE;
  }

  @AssertTrue(message = "date는 KST 기준 어제 이하여야 합니다.")
  private boolean isDateValid() {
    return date == null || !date.isAfter(LocalDate.now(TimeUtils.KST).minusDays(1));
  }
}
