package com.typingpractice.typing_practice_be.report.service;

import com.typingpractice.typing_practice_be.dailylimit.DailyLimitService;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.dto.ReportCreateRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportPaginationRequest;
import com.typingpractice.typing_practice_be.report.exception.DuplicateReportException;
import com.typingpractice.typing_practice_be.report.exception.QuoteNotReportableException;
import com.typingpractice.typing_practice_be.report.exception.ReportNotFoundException;
import com.typingpractice.typing_practice_be.report.exception.ReportNotProcessableException;
import com.typingpractice.typing_practice_be.report.repository.ReportRepository;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
  private final MemberRepository memberRepository;
  private final QuoteRepository quoteRepository;

  private final ReportRepository reportRepository;
  private final DailyLimitService dailyLimitService;

  // 신고 생성
  @Transactional
  public Report createReport(Long memberId, Long quoteId, ReportCreateRequest request) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
    Quote quote = quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);

    // 신고 가능 문장 검증
    if (quote.getStatus() != QuoteStatus.ACTIVE || quote.getType() != QuoteType.PUBLIC) {
      throw new QuoteNotReportableException();
    }

    // 신고 횟수 가능 검증
    if (!dailyLimitService.canReport(memberId)) {
      throw new IllegalStateException("하루 최대 신고 횟수 초과");
    }

    // 중복 신고 검증
    if (reportRepository.existsByQuoteAndMember(quote, member)) {
      throw new DuplicateReportException();
    }

    // 신고 생성
    Report report = Report.create(member, quote, request.getReason(), request.getDetail());

    reportRepository.save(report);

    // 문장, 회원 신고 횟수 증가
    quote.increaseReportCount();
    dailyLimitService.incrementReportCount(memberId);

    if (quote.shouldBeHidden()) {
      quote.updateStatus(QuoteStatus.HIDDEN);
    }

    return report;
  }

  // 내 신고 내역 조회
  // 페이징 추가 필요
  public List<Report> findMyReports(Long memberId, ReportPaginationRequest request) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    return reportRepository.findMyReports(member, request);
  }

  // 본인 신고 내역 삭제(철회 or 처리된 신고 삭제)
  @Transactional
  public void deleteReport(Long memberId, Long reportId) {
    Report report = reportRepository.findById(reportId).orElseThrow(ReportNotFoundException::new);

    if (!Objects.equals(report.getMember().getId(), memberId)) {
      throw new ReportNotProcessableException();
    }

    reportRepository.delete(report);
  }
}
