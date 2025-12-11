package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

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
      throw new IllegalStateException("개인용 문장만 수정 가능");
    }

    if (request.getAuthor() != null) {
      String author = StringUtils.hasText(request.getAuthor()) ? request.getAuthor() : "작자 미상";
      quote.updateAuthor(author);
    }

    if (request.getSentence() != null) {
      quote.updateSentence(request.getSentence());
    }

    return quote;
  }

  @Transactional
  public void deleteQuote(Long memberId, Long quoteId) {
    Quote quote = findMyQuoteById(memberId, quoteId);

    if (quote.getType() != QuoteType.PRIVATE || quote.getStatus() != QuoteStatus.ACTIVE) {
      throw new IllegalStateException("개인용 문장만 삭제 가능");
    }

    quoteRepository.deleteQuote(quote);
  }

  public List<Quote> getMyQuotes(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    return quoteRepository.findByMember(member);
  }

  private Quote findMyQuoteById(Long memberId, Long quoteId) {
    Quote quote = quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);

    if (!quote.getMember().getId().equals(memberId)) {
      throw new IllegalStateException("내 문장이 아닙니다.");
    }

    return quote;
  }

  @Transactional
  public Quote publishQuote(Long memberId, Long quoteId) {
    Quote quote = findMyQuoteById(memberId, quoteId);

    if (quote.getType() != QuoteType.PRIVATE || quote.getStatus() != QuoteStatus.ACTIVE) {
      throw new IllegalStateException("공개 전환할 수 없는 문장");
    }

    quote.updateType(QuoteType.PUBLIC);
    quote.updateStatus(QuoteStatus.PENDING);

    return quote;
  }

  @Transactional
  public Quote cancelPublishQuote(Long memberId, Long quoteId) {
    Quote quote = findMyQuoteById(memberId, quoteId);

    if (quote.getType() != QuoteType.PUBLIC || quote.getStatus() != QuoteStatus.PENDING) {
      throw new IllegalStateException("취소할 수 없는 문장");
    }

    quote.updateType(QuoteType.PRIVATE);
    quote.updateStatus(QuoteStatus.ACTIVE);

    return quote;
  }
}
