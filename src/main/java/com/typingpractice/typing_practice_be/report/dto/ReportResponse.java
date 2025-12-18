package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ReportResponse {
  private ReportReason reason;
  private ReportStatus status;
  private String detail;

  public static ReportResponse from(Report report) {
    ReportResponse response = new ReportResponse();
    response.reason = report.getReason();
    response.status = report.getStatus();
    response.detail = report.getDetail();

    return response;
  }
}
