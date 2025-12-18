package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ReportRequest {
  private ReportStatus status;

  public static ReportRequest create(ReportStatus status) {
    ReportRequest request = new ReportRequest();
    request.status = status;

    return request;
  }
}
