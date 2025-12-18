package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Getter
@ToString
public class ReportCreateRequest {
  private ReportReason reason;

  @Length(min = 1, max = 200)
  private String detail;

  public static ReportCreateRequest create(ReportReason reason, String detail) {
    ReportCreateRequest request = new ReportCreateRequest();
    request.reason = reason;
    request.detail = detail;

    return request;
  }
}
