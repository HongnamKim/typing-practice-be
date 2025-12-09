package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {
  private final QuoteRepository quoteRepository;
  private final MemberRepository memberRepository;

  public List<Quote> findAll() {
    List<Quote> all = quoteRepository.findAll();

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

  public List<Quote> findQuoteByStatus(QuoteStatus quoteStatus) {
    return quoteRepository.findByStatus(quoteStatus);
  }

  @Transactional
  public Quote updateQuoteStatus(Long quoteId, QuoteStatus quoteStatus) {
    Quote quote = quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);

    quote.updateStatus(quoteStatus);

    return quote;
  }

  @Transactional
  public void deleteQuote(Long quoteId) {
    Quote quote = quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);

    if (quote.getType() != QuoteType.PRIVATE) {
      throw new IllegalStateException("개인용 문장만 삭제 가능");
    }

    quoteRepository.deleteQuote(quote);
  }
}
