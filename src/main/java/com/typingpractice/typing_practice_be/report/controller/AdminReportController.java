package com.typingpractice.typing_practice_be.report.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.exception.NotAdminException;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.dto.ReportProcessRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportResponse;
import com.typingpractice.typing_practice_be.report.service.AdminReportService;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdminReportController {
  private final MemberService memberService;
  private final AdminReportService adminReportService;

  private void validateAdmin() {
    Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Member member = memberService.findMemberById(memberId);

    if (member.getRole() != MemberRole.ADMIN) {
      throw new NotAdminException();
    }
  }

  @GetMapping("/admin/reports")
  public ApiResponse<List<ReportResponse>> getReports(
      @ModelAttribute @Valid ReportRequest request) {
    validateAdmin();

    List<Report> reports = adminReportService.findReports(request);

    return ApiResponse.ok(reports.stream().map(ReportResponse::from).toList());
  }

  @PostMapping("/admin/reports/{quoteId}/process")
  public ApiResponse<Void> processReport(
      @PathVariable Long quoteId, @RequestBody @Valid ReportProcessRequest request) {
    validateAdmin();

    adminReportService.processReport(quoteId, request);

    return ApiResponse.ok(null);
  }

  @DeleteMapping("/admin/reports/{reportId}")
  public ApiResponse<Void> deleteReport(@PathVariable Long reportId) {
    validateAdmin();

    adminReportService.deleteReport(reportId);

    return ApiResponse.ok(null);
  }
}
