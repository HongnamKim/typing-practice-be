package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotProcessableException;
import com.typingpractice.typing_practice_be.quote.query.QuotePaginationQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteUpdateQuery;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.repository.ReportRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQuoteService {
  private final QuoteRepository quoteRepository;
  private final ReportRepository reportRepository;
  private final QuoteIdCacheService quoteIdCacheService;
  private final TypingRecordRepository typingRecordRepository;

  @Transactional
  public Quote approvePublish(Long quoteId) {
    Quote quote = findQuoteById(quoteId);

    if (quote.getStatus() != QuoteStatus.PENDING || quote.getType() != QuoteType.PUBLIC) {
      throw new QuoteNotProcessableException();
    }

    quote.approvePublish();

    return quote;
  }

  @Transactional
  public Quote rejectPublish(Long quoteId) {
    Quote quote = findQuoteById(quoteId);

    if (quote.getStatus() != QuoteStatus.PENDING || quote.getType() != QuoteType.PUBLIC) {
      throw new QuoteNotProcessableException();
    }

    quote.rejectPublish();

    return quote;
  }

  @Transactional
  public Quote updateQuote(Long quoteId, QuoteUpdateQuery query) {
    Quote quote = findQuoteById(quoteId);

    if (quote.getType() != QuoteType.PUBLIC) {
      throw new QuoteNotProcessableException();
    }

    quote.update(query.getSentence(), query.getAuthor());

    return quote;
  }

  private Quote findQuoteById(Long quoteId) {
    return quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);
  }

  @Transactional
  public void deleteQuote(Long quoteId) {
    Quote targetQuote = findQuoteById(quoteId);

    List<Report> reports = reportRepository.findByQuote(targetQuote);

    reports.forEach(report -> report.process(true));

    Long ownerId = targetQuote.getMember().getId();
    QuoteLanguage language = targetQuote.getLanguage();

    quoteRepository.deleteQuote(targetQuote);

    quoteIdCacheService.invalidateMemberIds(ownerId, language);
  }

  @Transactional
  public Quote cancelHidden(Long quoteId) {
    Quote quote = findQuoteById(quoteId);

    if (quote.getStatus() != QuoteStatus.HIDDEN) {
      throw new QuoteNotProcessableException();
    }

    quote.updateStatus(QuoteStatus.ACTIVE);

    return quote;
  }

  public PageResult<Quote> findQuotes(QuotePaginationQuery query) {
    List<Quote> quotes = quoteRepository.findAll(query);

    boolean hasNext = quotes.size() > query.getSize();
    List<Quote> content = hasNext ? quotes.subList(0, query.getSize()) : quotes;

    return new PageResult<>(content, query.getPage(), query.getSize(), hasNext);
  }

  public Quote findQuoteByIdWithTypingStats(Long quoteId) {
    return quoteRepository
        .findByIdWithTypingStats(quoteId)
        .orElseThrow(QuoteNotFoundException::new);
  }

  @Transactional
  public Quote hideQuote(Long quoteId) {
    Quote quote = quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);

    quote.updateStatus(QuoteStatus.HIDDEN);

    return quote;
  }

  public PageResult<Quote> findDeletedQuotes(QuotePaginationQuery query) {
    List<Quote> deletedQuotes = quoteRepository.findDeletedQuotes(query);
    boolean hasNext = deletedQuotes.size() > query.getSize();
    List<Quote> content = hasNext ? deletedQuotes.subList(0, query.getSize()) : deletedQuotes;

    return new PageResult<>(content, query.getPage(), query.getSize(), hasNext);
  }

  @Transactional
  public void permanentDeleteQuote(Long quoteId) {
    Quote quote =
        quoteRepository.findDeletedQuoteById(quoteId).orElseThrow(QuoteNotFoundException::new);

    Long ownerId = quote.getMember().getId();
    QuoteLanguage language = quote.getLanguage();

    typingRecordRepository.deleteByQuoteId(quoteId);

    quoteRepository.permanentDeleteQuote(quoteId);

    quoteIdCacheService.invalidateMemberIds(ownerId, language);
  }
}
