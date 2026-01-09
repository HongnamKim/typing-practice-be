package com.typingpractice.typing_practice_be.report.service;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.dailylimit.DailyLimitService;
import com.typingpractice.typing_practice_be.dailylimit.exception.DailyReportLimitException;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.exception.DuplicateReportException;
import com.typingpractice.typing_practice_be.report.exception.QuoteNotReportableException;
import com.typingpractice.typing_practice_be.report.exception.ReportNotFoundException;
import com.typingpractice.typing_practice_be.report.exception.ReportNotProcessableException;
import com.typingpractice.typing_practice_be.report.query.ReportCreateQuery;
import com.typingpractice.typing_practice_be.report.query.ReportPaginationQuery;
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
  public Report createReport(Long memberId, Long quoteId, ReportCreateQuery query) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
    Quote quote = quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);

    // 신고 가능 문장 검증
    if (quote.getStatus() != QuoteStatus.ACTIVE || quote.getType() != QuoteType.PUBLIC) {
      throw new QuoteNotReportableException();
    }

    // 신고 횟수 가능 검증
    if (!dailyLimitService.canReport(memberId)) {
      throw new DailyReportLimitException();
    }

    // 중복 신고 검증
    if (reportRepository.existsByQuoteAndMember(quote, member)) {
      throw new DuplicateReportException();
    }

    // 신고 생성
    Report report = Report.create(member, quote, query.getReason(), query.getDetail());

    reportRepository.save(report);

    // 문장, 회원 신고 횟수 증가
    quote.increaseReportCount();
    dailyLimitService.incrementReportCount(memberId);

    if (quote.shouldBeHidden()) {
      quote.updateStatus(QuoteStatus.HIDDEN);
    }

    return report;
  }

  public PageResult<Report> findMyReports(Long memberId, ReportPaginationQuery query) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    List<Report> myReports = reportRepository.findMyReports(member, query);

    boolean hasNext = myReports.size() > query.getSize();
    List<Report> content = hasNext ? myReports.subList(0, query.getSize()) : myReports;

    return new PageResult<>(content, query.getPage(), query.getSize(), hasNext);
  }

  // 본인 신고 내역 삭제(철회 or 처리된 신고 삭제)
  @Transactional
  public void deleteReport(Long memberId, Long reportId) {
    Report report = reportRepository.findById(reportId).orElseThrow(ReportNotFoundException::new);

    if (!Objects.equals(report.getMember().getId(), memberId)) {
      throw new ReportNotProcessableException();
    }

    // 처리되지 않은 신고를 삭제할 경우 신고 횟수 차감
    if (report.getStatus() != ReportStatus.PROCESSED) {
      Quote quote = report.getQuote();

      quote.decreaseReportCount();
    }

    reportRepository.delete(report);
  }
}
