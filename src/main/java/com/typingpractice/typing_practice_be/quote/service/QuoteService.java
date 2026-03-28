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

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.typingpractice.typing_practice_be.quote.service.difficulty.DifficultySeedCalculator;
import com.typingpractice.typing_practice_be.quote.service.difficulty.QuoteProfileCalculator;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.quote.statistics.service.GlobalQuoteStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
	private final QuoteIdCacheService quoteIdCacheService;

	public PageResult<Quote> findRandomPublicQuotes(PublicQuoteQuery query) {
		long t1 = System.currentTimeMillis();

		QuoteLanguage language = query.getLanguage();
		Random random = new Random((long) (query.getSeed() * 1_000));

		List<Long> ids;
		if (query.getOnlyMyQuotes() && query.getMemberId() != null) {
			// 로그인한 유저의 내 문장만 조회
			ids = quoteIdCacheService.getIdsByMemberId(query.getMemberId(), language);
		} else {
			// 전체 문장 랜덤 조회
			ids = quoteIdCacheService.getPublicIds(language, QuoteDifficultyTier.ALL);
		}

		long t2 = System.currentTimeMillis();
		log.info("[성능] 캐시 조회: {}ms", t2 - t1);

		Collections.shuffle(ids, random);

		long t3 = System.currentTimeMillis();
		log.info("[성능] 셔플: {}ms", t3 - t2);

		int from = (query.getPage() - 1) * query.getCount();
		int to = Math.min(from + query.getCount() + 1, ids.size());

		if (from >= ids.size()) {
			return new PageResult<>(List.of(), query.getPage(), query.getCount(), false);
		}

		List<Long> slicedIds = ids.subList(from, to);
		List<Quote> fetched = quoteRepository.findByIds(slicedIds, language);

		long t4 = System.currentTimeMillis();
		log.info("[성능] DB 조회: {}ms", t4 - t3);

		boolean hasNext = fetched.size() > query.getCount();
		List<Quote> content = hasNext ? fetched.subList(0, query.getCount()) : fetched;

		return new PageResult<>(content, query.getPage(), query.getCount(), hasNext);

//		long t3 = System.currentTimeMillis();
//		List<Quote> quotes = quoteRepository.findPublicQuotes(query);
//		long t4 = System.currentTimeMillis();
//		log.info("[성능] DB 조회: {}ms", t4 - t3);
//
//		boolean hasNext = quotes.size() > query.getCount();
//		List<Quote> content = hasNext ? quotes.subList(0, query.getCount()) : quotes;
//
//		return new PageResult<>(content, query.getPage(), query.getCount(), hasNext);
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
		quote.updateDynamicDifficulty(seed); // 통계 없을 때 초기 난이도
		quoteRepository.save(quote);

		quoteIdCacheService.invalidateMemberIds(memberId, language);

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

		quoteIdCacheService.invalidateMemberIds(memberId, quote.getLanguage());
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

		quoteIdCacheService.invalidateMemberIds(memberId, quote.getLanguage());

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

		quoteIdCacheService.invalidateMemberIds(memberId, quote.getLanguage());

		return quote;
	}
}
