package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuotePaginationRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotOwnedException;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotProcessableException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {
  private final QuoteRepository quoteRepository;
  private final MemberRepository memberRepository;

  public List<Quote> findPublicQuotes() {
    List<Quote> all = quoteRepository.findPublicQuotes();

    return all;
  }

  public Quote findById(Long quoteId) {
    return quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);
  }

  @Transactional
  public Quote create(Long memberId, QuoteCreateRequest request) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
    Quote quote =
        Quote.create(member, request.getSentence(), request.getAuthor(), request.getType());

    quoteRepository.save(quote);

    return quote;
  }

  @Transactional
  public Quote updatePrivateQuote(Long memberId, Long quoteId, QuoteUpdateRequest request) {
    Quote quote = findMyQuoteById(memberId, quoteId);

    if (quote.getType() != QuoteType.PRIVATE || quote.getStatus() != QuoteStatus.ACTIVE) {
      throw new QuoteNotProcessableException();
    }

    quote.update(request.getSentence(), request.getAuthor());

    return quote;
  }

  @Transactional
  public void deleteQuote(Long memberId, Long quoteId) {
    Quote quote = findMyQuoteById(memberId, quoteId);

    if (quote.getType() != QuoteType.PRIVATE || quote.getStatus() != QuoteStatus.ACTIVE) {
      throw new QuoteNotProcessableException();
    }

    quoteRepository.deleteQuote(quote);
  }

  public List<Quote> getMyQuotes(Long memberId, QuotePaginationRequest request) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    return quoteRepository.findByMember(member, request);
  }

  private Quote findMyQuoteById(Long memberId, Long quoteId) {
    Quote quote = quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);

    if (!quote.getMember().getId().equals(memberId)) {
      throw new QuoteNotOwnedException();
    }

    return quote;
  }

  @Transactional
  public Quote publishQuote(Long memberId, Long quoteId) {
    Quote quote = findMyQuoteById(memberId, quoteId);

    if (quote.getType() != QuoteType.PRIVATE || quote.getStatus() != QuoteStatus.ACTIVE) {
      throw new QuoteNotProcessableException();
    }

    quote.updateType(QuoteType.PUBLIC);
    quote.updateStatus(QuoteStatus.PENDING);

    return quote;
  }

  @Transactional
  public Quote cancelPublishQuote(Long memberId, Long quoteId) {
    Quote quote = findMyQuoteById(memberId, quoteId);

    if (quote.getType() != QuoteType.PUBLIC || quote.getStatus() != QuoteStatus.PENDING) {
      throw new QuoteNotProcessableException();
    }

    quote.updateType(QuoteType.PRIVATE);
    quote.updateStatus(QuoteStatus.ACTIVE);

    return quote;
  }
}
