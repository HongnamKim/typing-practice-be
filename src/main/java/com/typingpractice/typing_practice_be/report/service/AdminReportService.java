package com.typingpractice.typing_practice_be.report.service;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.dto.ReportProcessRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportRequest;
import com.typingpractice.typing_practice_be.report.exception.ReportNotFoundException;
import com.typingpractice.typing_practice_be.report.repository.ReportRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {
  private final QuoteRepository quoteRepository;
  private final ReportRepository reportRepository;

  public List<Report> findReports(ReportRequest request) {
    List<Report> all = reportRepository.findAll(request);

    return all;
  }

  @Transactional
  public void processReport(Long quoteId, ReportProcessRequest request) {
    Quote targetQuote = quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);

    // DTO 에 값이 없으면 삭제
    if (request.getSentence() == null && request.getAuthor() == null) {
      quoteRepository.deleteQuote(targetQuote);

    } else {
      // DTO 에 값이 있으면 수정
      targetQuote.update(request.getSentence(), request.getAuthor());
      targetQuote.resetReportCount();

      reportRepository.processReportByQuote(targetQuote);
    }
  }

  @Transactional
  public void deleteReport(Long reportId) {
    Report report = reportRepository.findById(reportId).orElseThrow(ReportNotFoundException::new);

    reportRepository.delete(report);
  }
}
