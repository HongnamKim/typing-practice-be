package com.typingpractice.typing_practice_be.report.service;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.exception.ReportNotFoundException;
import com.typingpractice.typing_practice_be.report.query.ReportPaginationQuery;
import com.typingpractice.typing_practice_be.report.query.ReportProcessQuery;
import com.typingpractice.typing_practice_be.report.repository.ReportRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {
  private final MemberRepository memberRepository;
  private final QuoteRepository quoteRepository;
  private final ReportRepository reportRepository;

  public PageResult<Report> findReports(ReportPaginationQuery query) {

    if (query.getMemberId() != null) {
      memberRepository.findById(query.getMemberId()).orElseThrow(MemberNotFoundException::new);
    }

    List<Report> reports = reportRepository.findAll(query);

    boolean hasNext = reports.size() > query.getSize();
    List<Report> content = hasNext ? reports.subList(0, query.getSize()) : reports;

    return new PageResult<>(content, query.getPage(), query.getSize(), hasNext);
  }

  @Transactional
  public void processReport(Long quoteId, ReportProcessQuery query) {
    Quote targetQuote = quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);

    List<Report> reports = reportRepository.findByQuote(targetQuote);

    // DTO 에 값이 없으면 삭제
    if (query.getSentence() == null && query.getAuthor() == null) {
      quoteRepository.deleteQuote(targetQuote);

      // 신고 내역 처리
      // reportRepository.processReportByQuote(targetQuote, true);
      reports.forEach(report -> report.process(true));
    } else {
      // DTO 에 값이 있으면 수정
      targetQuote.update(query.getSentence(), query.getAuthor());
      targetQuote.resetReportCount();

      // 신고 내역 처리
      // reportRepository.processReportByQuote(targetQuote, false);
      reports.forEach(report -> report.process(false));
    }
  }

  @Transactional
  public void deleteReport(Long reportId) {
    Report report = reportRepository.findById(reportId).orElseThrow(ReportNotFoundException::new);

    if (report.getStatus() != ReportStatus.PROCESSED) {
      Quote quote = report.getQuote();
      quote.decreaseReportCount();
    }

    reportRepository.delete(report);
  }
}
