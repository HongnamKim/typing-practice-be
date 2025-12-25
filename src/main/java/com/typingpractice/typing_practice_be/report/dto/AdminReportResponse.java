package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.member.dto.MemberResponse;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class AdminReportResponse {
  private Long id;
  private ReportReason reason;
  private ReportStatus status;
  private String detail;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private MemberResponse member;

  public static AdminReportResponse from(Report report, MemberResponse member) {
    AdminReportResponse response = new AdminReportResponse();

    response.id = report.getId();
    response.reason = report.getReason();
    response.status = report.getStatus();
    response.detail = report.getDetail();

    response.createdAt = report.getCreatedAt();
    response.updatedAt = report.getUpdatedAt();

    response.member = member;

    return response;
  }
}
