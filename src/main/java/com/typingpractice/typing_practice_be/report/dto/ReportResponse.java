package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
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
  private boolean quoteDeleted;
  private String detail;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private QuoteResponse quote;

  public static ReportResponse from(Report report, QuoteResponse quote) {
    ReportResponse response = new ReportResponse();
    response.id = report.getId();
    response.reason = report.getReason();
    response.status = report.getStatus();
    response.quoteDeleted = report.isQuoteDeleted();
    response.detail = report.getDetail();

    response.createdAt = report.getCreatedAt();
    response.updatedAt = report.getUpdatedAt();

    response.quote = quote;

    return response;
  }
}
