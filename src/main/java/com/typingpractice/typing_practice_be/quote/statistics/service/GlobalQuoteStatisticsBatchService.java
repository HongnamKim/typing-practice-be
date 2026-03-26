package com.typingpractice.typing_practice_be.quote.statistics.service;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.quote.service.difficulty.DifficultySeedCalculator;
import com.typingpractice.typing_practice_be.quote.service.difficulty.QuoteProfileCalculator;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.quote.statistics.dto.QuoteProfileAggregation;
import com.typingpractice.typing_practice_be.quote.statistics.repository.GlobalQuoteStatisticsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.GlobalTypingPerformance;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.QuoteTypingStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalQuoteStatisticsBatchService {
	private final GlobalQuoteStatisticsRepository statsRepository;
	private final GlobalQuoteStatisticsService statsService;
	private final QuoteRepository quoteRepository;
	private final DifficultySeedCalculator seedCalculator;
	private final QuoteProfileCalculator quoteProfileCalculator;
	private final QuoteTypingStatsRepository quoteTypingStatsRepository;

	private static final float CHANGE_THRESHOLD = 0.05f;
	private static final int PAGE_SIZE = 5000;

	@Transactional
	public void runScheduledBatch() {
		for (QuoteLanguage lang : QuoteLanguage.values()) {
			GlobalQuoteStatistics prev = statsService.getByLanguage(lang);
			// 일별 전체 문장의 평균 코퍼스, 타이핑 속도, 정확도 계산
			GlobalQuoteStatistics next = recalculateStats(lang);

			// 평균 코퍼스 변화가 클 경우 seed 재계산
			if (shouldRecalculateSeeds(prev, next, lang)) {
				log.info("[{}] 변화율 임계값 초과 -> seed 재계산 시작", lang);
				recalculateAllSeeds(lang, next);
			} else {
				log.info("[{}] 변화율 임계값 미달 -> seed 재계산 스킵", lang);
			}
		}

		statsService.refreshCache();
	}

	@Transactional
	public void runManualRecalculation() {
		for (QuoteLanguage lang : QuoteLanguage.values()) {
			GlobalQuoteStatistics next = recalculateStats(lang);
			log.info("[{}] 수동 트리거 -> seed 재계산 시작", lang);
			recalculateAllSeeds(lang, next);
		}

		statsService.refreshCache();
	}

	private GlobalQuoteStatistics recalculateStats(QuoteLanguage lang) {
		QuoteProfileAggregation statsAgg = statsRepository.aggregateByLanguage(lang);
		GlobalQuoteStatistics next = GlobalQuoteStatistics.createFromAggregation(lang, statsAgg);

		// 전역 타이핑 결과 갱신
		GlobalTypingPerformance perf = quoteTypingStatsRepository.aggregateGlobalAvgByLanguage(lang);
		if (!perf.isEmpty()) {
			next.updateGlobalTypingPerformance(perf.getAvgCpm(), perf.getAvgAcc());
		}

		statsRepository.save(next);
		log.info(
						"[{}] 전역 통계 재계산 완료 - lenMean = {}, puncMean = {}",
						lang,
						next.getLenMean(),
						next.getPuncMean());

		return next;
	}

	private boolean shouldRecalculateSeeds(
					GlobalQuoteStatistics prev, GlobalQuoteStatistics next, QuoteLanguage lang) {
		if (prev == null) return true;

		// 공통 μ 비교
		if (exceedsThreshold(prev.getLenMean(), next.getLenMean())) return true;
		if (exceedsThreshold(prev.getPuncMean(), next.getPuncMean())) return true;
		if (exceedsThreshold(prev.getSpaceMean(), next.getSpaceMean())) return true;
		if (exceedsThreshold(prev.getDigitMean(), next.getDigitMean())) return true;

		// 언어 전용 μ 비교
		if (lang == QuoteLanguage.KOREAN) {
			if (exceedsThreshold(prev.getJamoMean(), next.getJamoMean())) return true;
			if (exceedsThreshold(prev.getDiphthongMean(), next.getDiphthongMean())) return true;
			if (exceedsThreshold(prev.getShiftJamoMean(), next.getShiftJamoMean())) return true;
		} else {
			if (exceedsThreshold(prev.getCaseMean(), next.getCaseMean())) return true;
			if (exceedsThreshold(prev.getWordLenMean(), next.getWordLenMean())) return true;
		}

		return false;
	}

	private boolean exceedsThreshold(Float prev, Float next) {
		if (prev == null || next == null) return prev != next;
		if (prev == 0f) return next != 0f;
		return Math.abs(next - prev) / Math.abs(prev) > CHANGE_THRESHOLD;
	}

	private void recalculateAllSeeds(QuoteLanguage lang, GlobalQuoteStatistics stats) {
		Long maxId = quoteRepository.findMaxIdByLanguage(lang);
		if (maxId == null) return;

		Long cursor = 0L;
		int totalUpdated = 0;

		while (true) {
			List<Quote> quotes =
							quoteRepository.findPageByLanguageAndIdRange(lang, cursor, maxId, PAGE_SIZE);
			if (quotes.isEmpty()) break;

			for (Quote quote : quotes) {
				if (quote.getProfile() == null) {
					QuoteProfile profile =
									quoteProfileCalculator.calculate(quote.getSentence(), quote.getLanguage());

					quote.updateProfile(profile);
				}

				float seed = seedCalculator.calculate(quote.getProfile(), stats, lang);
				quote.updateDifficultySeed(seed);
			}

			totalUpdated += quotes.size();
			cursor = quotes.getLast().getId();
		}

		log.info("[{}] seed 재계산 완료 - {}건 갱신", lang, totalUpdated);
	}
}
