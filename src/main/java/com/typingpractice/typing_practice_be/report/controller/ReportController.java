package com.typingpractice.typing_practice_be.report.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.dto.ReportCreateRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportPaginationRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportPaginationResponse;
import com.typingpractice.typing_practice_be.report.dto.ReportResponse;
import com.typingpractice.typing_practice_be.report.query.ReportCreateQuery;
import com.typingpractice.typing_practice_be.report.query.ReportPaginationQuery;
import com.typingpractice.typing_practice_be.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReportController {
  private final ReportService reportService;

  private Long getMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }

  // @BannedNotAllowed
  @PostMapping("/reports")
  public ApiResponse<ReportResponse> reportQuote(@RequestBody @Valid ReportCreateRequest request) {
    Long memberId = getMemberId();

    ReportCreateQuery query = ReportCreateQuery.from(request);

    Report report = reportService.createReport(memberId, request.getQuoteId(), query);

    QuoteResponse quote = QuoteResponse.from(report.getQuote());

    return ApiResponse.ok(ReportResponse.from(report, quote));
  }

  @GetMapping("/reports/my")
  public ApiResponse<ReportPaginationResponse> getMyReports(
      @ModelAttribute @Valid ReportPaginationRequest request) {
    Long memberId = getMemberId();

    ReportPaginationQuery query = ReportPaginationQuery.from(request);

    PageResult<Report> result = reportService.findMyReports(memberId, query);

    return ApiResponse.ok(ReportPaginationResponse.from(result));
  }

  @DeleteMapping("/reports/{reportId}")
  public ApiResponse<Void> deleteMyReport(@PathVariable Long reportId) {
    Long memberId = getMemberId();

    reportService.deleteReport(memberId, reportId);

    return ApiResponse.ok(null);
  }
}
