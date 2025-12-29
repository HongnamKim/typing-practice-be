package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.common.dto.PaginationResponse;
import com.typingpractice.typing_practice_be.member.dto.MemberResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.report.domain.Report;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AdminReportPaginationResponse extends PaginationResponse {
  private List<AdminReportResponse> content;

  protected AdminReportPaginationResponse(int page, int size, boolean hasNext) {
    super(page, size, hasNext);
  }

  public static AdminReportPaginationResponse from(
      List<Report> reports, int page, int size, boolean hasNext) {
    AdminReportPaginationResponse response = new AdminReportPaginationResponse(page, size, hasNext);

    response.content =
        reports.stream()
            .limit(size)
            .map(
                r -> {
                  MemberResponse member = MemberResponse.from(r.getMember());
                  QuoteResponse quote =
                      r.getQuote() != null ? QuoteResponse.from(r.getQuote()) : null;
                  // System.out.println(r.getQuote());

                  return AdminReportResponse.from(r, member, quote);
                })
            .toList();

    return response;
  }
}
