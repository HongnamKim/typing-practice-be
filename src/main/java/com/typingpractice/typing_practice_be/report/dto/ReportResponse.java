package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ReportResponse {
  private Long id;
  private ReportReason reason;
  private ReportStatus status;
  private String detail;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static ReportResponse from(Report report) {
    ReportResponse response = new ReportResponse();
    response.id = report.getId();
    response.reason = report.getReason();
    response.status = report.getStatus();
    response.detail = report.getDetail();

    response.createdAt = report.getCreatedAt();
    response.updatedAt = report.getUpdatedAt();

    return response;
  }
}
