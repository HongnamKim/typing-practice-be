package com.typingpractice.typing_practice_be.report.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.dto.ReportCreateRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportResponse;
import com.typingpractice.typing_practice_be.report.service.ReportService;
import java.util.List;

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

  @PostMapping("/reports")
  public ApiResponse<ReportResponse> reportQuote(@RequestBody @Valid ReportCreateRequest request) {
    Long memberId = getMemberId();

    Report report = reportService.createReport(memberId, request.getQuoteId(), request);

    return ApiResponse.ok(ReportResponse.from(report));
  }

  @GetMapping("/reports/my")
  public ApiResponse<List<ReportResponse>> getMyReports() {
    Long memberId = getMemberId();

    List<Report> myReports = reportService.findMyReports(memberId);

    return ApiResponse.ok(myReports.stream().map(ReportResponse::from).toList());
  }

  @DeleteMapping("/reports/{reportId}")
  public ApiResponse<Void> deleteMyReport(@PathVariable Long reportId) {
    Long memberId = getMemberId();

    reportService.deleteReport(memberId, reportId);

    return ApiResponse.ok(null);
  }
}
