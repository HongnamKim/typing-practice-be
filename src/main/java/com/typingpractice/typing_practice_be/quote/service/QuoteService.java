package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.dailylimit.DailyLimitService;
import com.typingpractice.typing_practice_be.dailylimit.exception.DailyQuoteUploadLimitException;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.*;
import com.typingpractice.typing_practice_be.quote.exception.*;
import com.typingpractice.typing_practice_be.quote.query.PublicQuoteQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteCreateQuery;
import com.typingpractice.typing_practice_be.quote.query.QuotePaginationQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteUpdateQuery;
import com.typingpractice.typing_practice_be.quote.reject.service.QuoteSimilarityRejectService;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import java.util.List;

import com.typingpractice.typing_practice_be.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.statistics.service.GlobalQuoteStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {
  private final QuoteLanguageValidator quoteLanguageValidator;
  private final SentenceHashGenerator sentenceHashGenerator;
  private final QuoteProfileCalculator quoteProfileCalculator;
  private final DifficultySeedCalculator difficultySeedCalculator;
  private final GlobalQuoteStatisticsService globalQuoteStatisticsService;

  private final QuoteSimilarityRejectService rejectService;

  private final QuoteRepository quoteRepository;
  private final MemberRepository memberRepository;

  private final DailyLimitService dailyLimitService;

  public PageResult<Quote> findRandomPublicQuotes(PublicQuoteQuery query) {
    List<Quote> quotes = quoteRepository.findPublicQuotes(query);

    boolean hasNext = quotes.size() > query.getCount();
    List<Quote> content = hasNext ? quotes.subList(0, query.getCount()) : quotes;

    return new PageResult<>(content, query.getPage(), query.getCount(), hasNext);
  }

  public Quote findById(Long quoteId) {
    return quoteRepository.findById(quoteId).orElseThrow(QuoteNotFoundException::new);
  }

  @Transactional
  public Quote create(Long memberId, QuoteCreateQuery query) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    if (!dailyLimitService.tryIncrementQuoteUploadCount(memberId)) {
      throw new DailyQuoteUploadLimitException();
    }

    String sentence = query.getSentence();
    QuoteLanguage language = query.getLanguage();
    // 언어 검증
    quoteLanguageValidator.validate(sentence, language);

    // 중복 검증
    String sentenceHash = sentenceHashGenerator.generate(sentence);
    if (query.getType() == QuoteType.PUBLIC) {
      // 공개 문장 업로드

      // 동일 문장 검증
      if (quoteRepository.existsBySentenceHash(sentenceHash, memberId)) {
        throw new QuoteDuplicateException();
      }

      // 유사 문장 검증
      quoteRepository
          .findMostSimilar(sentence, language, memberId)
          .ifPresent(
              row -> {
                String similar = (String) row[0];
                float sim = ((Number) row[1]).floatValue();
                rejectService.log(memberId, sentence, similar, sim, language);

                throw new QuoteSimilarException(similar, sim);
              });

    } else if (query.getType() == QuoteType.PRIVATE) {
      // 비공개 문장 업로드

      // 동일 문장 검증
      if (quoteRepository.existsBySentenceHashInMyQuotes(sentenceHash, memberId)) {
        throw new QuoteDuplicateException();
      }

      // 유사 문장 검증
      quoteRepository
          .findMostSimilarInMyQuotes(sentence, language, memberId)
          .ifPresent(
              row -> {
                String similar = (String) row[0];
                float sim = ((Number) row[1]).floatValue();
                rejectService.log(memberId, sentence, similar, sim, language);

                throw new QuoteSimilarException(similar, sim);
              });
    }

    // 입력 변수
    QuoteProfile profile = quoteProfileCalculator.calculate(sentence, language);
    // 전역 통계
    GlobalQuoteStatistics stats = globalQuoteStatisticsService.getByLanguage(language);

    // difficulty seed 계산
    float seed = difficultySeedCalculator.calculate(profile, stats, language);
    profile.setDifficultySeed(seed);

    Quote quote =
        Quote.create(
            member,
            query.getSentence(),
            query.getAuthor(),
            query.getType(),
            query.getLanguage(),
            profile,
            seed,
            sentenceHash);

    quoteRepository.save(quote);

    return quote;
  }

  @Transactional
  public Quote updatePrivateQuote(Long memberId, Long quoteId, QuoteUpdateQuery query) {
    Quote quote = findMyQuoteById(memberId, quoteId);

    if (quote.getType() != QuoteType.PRIVATE || quote.getStatus() != QuoteStatus.ACTIVE) {
      throw new QuoteNotProcessableException();
    }

    if (query.getSentence() != null) {
      String sentenceHash = sentenceHashGenerator.generate(query.getSentence());
      // 동일 문장 검증
      if (quoteRepository.existsBySentenceHashInMyQuotesExcluding(
          sentenceHash, memberId, quoteId)) {
        throw new QuoteDuplicateException();
      }

      // 유사도 검증
      quoteRepository
          .findMostSimilarInMyQuotesExcluding(
              query.getSentence(), quote.getLanguage(), memberId, quoteId)
          .ifPresent(
              row -> {
                String similar = (String) row[0];
                float sim = ((Number) row[1]).floatValue();
                rejectService.log(memberId, query.getSentence(), similar, sim, quote.getLanguage());

                throw new QuoteSimilarException(similar, sim);
              });

      quote.updateSentenceHash(sentenceHash);
    }

    quote.update(query.getSentence(), query.getAuthor());

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

  public PageResult<Quote> getMyQuotes(Long memberId, QuotePaginationQuery query) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    List<Quote> quotes = quoteRepository.findByMember(member, query);

    boolean hasNext = quotes.size() > query.getSize();
    List<Quote> content = hasNext ? quotes.subList(0, query.getSize()) : quotes;

    return new PageResult<>(content, query.getPage(), query.getSize(), hasNext);
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

    if (quoteRepository.existsBySentenceHashExcluding(quote.getSentenceHash(), memberId, quoteId)) {
      throw new QuoteDuplicateException();
    }

    quoteRepository
        .findMostSimilarExcluding(quote.getSentence(), quote.getLanguage(), memberId, quoteId)
        .ifPresent(
            row -> {
              String similar = (String) row[0];
              float sim = ((Number) row[1]).floatValue();
              rejectService.log(memberId, quote.getSentence(), similar, sim, quote.getLanguage());

              throw new QuoteSimilarException(similar, sim);
            });

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
