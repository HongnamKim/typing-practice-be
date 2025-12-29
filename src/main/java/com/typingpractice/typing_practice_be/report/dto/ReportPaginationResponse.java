package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.common.dto.PaginationResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.report.domain.Report;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ReportPaginationResponse extends PaginationResponse {
  private List<ReportResponse> content;

  protected ReportPaginationResponse(int page, int size, boolean hasNext) {
    super(page, size, hasNext);
  }

  public static ReportPaginationResponse from(
      List<Report> reports, int page, int size, boolean hasNext) {
    ReportPaginationResponse response = new ReportPaginationResponse(page, size, hasNext);

    response.content =
        reports.stream()
            .limit(size)
            .map(
                r -> {
                  QuoteResponse quote =
                      r.getQuote() != null ? QuoteResponse.from(r.getQuote()) : null;

                  return ReportResponse.from(r, quote);
                })
            .toList();

    return response;
  }
}
