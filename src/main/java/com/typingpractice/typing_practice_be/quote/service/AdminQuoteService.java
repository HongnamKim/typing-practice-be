package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotProcessableException;
import com.typingpractice.typing_practice_be.quote.query.QuotePaginationQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteUpdateQuery;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQuoteService {
  private final QuoteRepository quoteRepository;

  //  public List<Quote> findPendingQuotes() {
  //    List<Quote> pendingQuotes = quoteRepository.findByStatus(QuoteStatus.PENDING);
  //
  //    return pendingQuotes;
  //  }

  //  public List<Quote> findHiddenQuotes() {
  //    List<Quote> hiddenQuotes = quoteRepository.findByStatus(QuoteStatus.HIDDEN);
  //
  //    return hiddenQuotes;
  //  }

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
    quoteRepository.deleteQuote(findQuoteById(quoteId));
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
}
