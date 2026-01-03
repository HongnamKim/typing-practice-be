package com.typingpractice.typing_practice_be.report.query;

import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import com.typingpractice.typing_practice_be.report.dto.ReportCreateRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ReportCreateQuery {
  private final ReportReason reason;
  private final String detail;

  private ReportCreateQuery(ReportReason reason, String detail) {
    this.reason = reason;
    this.detail = detail;
  }

  public static ReportCreateQuery from(ReportCreateRequest request) {
    return new ReportCreateQuery(request.getReason(), request.getDetail());
  }
}
