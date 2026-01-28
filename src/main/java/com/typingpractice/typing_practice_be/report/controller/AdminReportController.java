package com.typingpractice.typing_practice_be.report.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.dto.*;
import com.typingpractice.typing_practice_be.report.query.ReportPaginationQuery;
import com.typingpractice.typing_practice_be.report.query.ReportProcessQuery;
import com.typingpractice.typing_practice_be.report.service.AdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
// @AdminOnly
public class AdminReportController {

  private final AdminReportService adminReportService;

  @GetMapping("/admin/reports")
  public ApiResponse<AdminReportPaginationResponse> getReports(
      @ModelAttribute @Valid ReportPaginationRequest request) {

    ReportPaginationQuery query = ReportPaginationQuery.from(request);

    PageResult<Report> result = adminReportService.findReports(query);

    return ApiResponse.ok(AdminReportPaginationResponse.from(result));
  }

  @GetMapping("/admin/reports/{reportId}")
  public ApiResponse<ReportResponse> getReportById(@PathVariable Long reportId) {
    Report report = adminReportService.findReportById(reportId);

    QuoteResponse quoteResponse =
        report.isQuoteDeleted() ? null : QuoteResponse.from(report.getQuote());

    return ApiResponse.ok(ReportResponse.from(report, quoteResponse));
  }

  @PostMapping("/admin/reports/{quoteId}/process")
  public ApiResponse<Void> processReport(
      @PathVariable Long quoteId, @RequestBody @Valid ReportProcessRequest request) {

    ReportProcessQuery query = ReportProcessQuery.from(request);

    adminReportService.processReport(quoteId, query);

    return ApiResponse.ok(null);
  }

  @DeleteMapping("/admin/reports/{reportId}")
  public ApiResponse<Void> deleteReport(@PathVariable Long reportId) {

    adminReportService.deleteReport(reportId);

    return ApiResponse.ok(null);
  }
}
