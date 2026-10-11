package com.typingpractice.typing_practice_be.quote.statistics.service;

import com.typingpractice.typing_practice_be.quote.config.DifficultyProperties;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.PerfDistribution;
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
	private final DifficultyProperties difficultyProperties; // perf 표본 조건 validAttempts ≥ k

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
				log.info("[{}] 변화율 임계값 미달 -> seed 재계산 스킵, 전날 seed 통계 유지", lang);
				next.copySeedDistributionFrom(prev); // 복사하지 않으면 다음 날 캐시된 최신 행에서 값이 사라진다
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

		// 전역 타이핑 결과 갱신 + 동적 보정 신호(perfRaw) 분포
		GlobalTypingPerformance perf = quoteTypingStatsRepository.aggregateGlobalAvgByLanguage(lang);
		if (!perf.isEmpty()) {
			next.updateGlobalTypingPerformance(perf.getAvgCpm(), perf.getAvgAcc());

			PerfDistribution dist =
							quoteTypingStatsRepository.aggregatePerfDistributionByLanguage(
											lang, perf.getAvgCpm(), perf.getAvgAcc(), difficultyProperties.getColdStartK());
			if (dist.isUsable()) {
				next.updatePerfDistribution(dist.getMean(), dist.getStd());
			} else {
				log.info("[{}] perf 표준화 표본 부족 -> 기본값 유지 ({})", lang, dist);
			}
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
		// 전날 행이 없거나 seed 표준화 통계가 비어 있으면(마이그레이션 직후 등) 특성 변화와 무관하게 재계산해 채운다
		if (prev == null || !prev.hasSeedDistribution()) return true;

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

		// 1차 패스: 공개·활성 문장의 rawSeed 분포(평균, 표준편차) → stats 에 저장
		RawSeedAccumulator acc = new RawSeedAccumulator();
		Long cursor = 0L;
		while (true) {
			List<Quote> quotes =
							quoteRepository.findPageByLanguageAndIdRange(lang, cursor, maxId, PAGE_SIZE);
			if (quotes.isEmpty()) break;

			for (Quote quote : quotes) {
				ensureProfile(quote);
				if (quote.getType() == QuoteType.PUBLIC && quote.getStatus() == QuoteStatus.ACTIVE) {
					acc.add(seedCalculator.calculateRaw(quote.getProfile(), stats, lang));
				}
			}
			cursor = quotes.getLast().getId();
		}

		if (acc.isUsable()) {
			stats.updateSeedDistribution(acc.mean(), acc.std());
			log.info("[{}] rawSeed 분포 - n={}, mean={}, std={}", lang, acc.count, acc.mean(), acc.std());
		} else {
			log.info("[{}] rawSeed 표준화 표본 부족 -> 기본값 유지 (n={})", lang, acc.count);
		}

		// 2차 패스: 표준화된 seed 저장 (전체 문장)
		cursor = 0L;
		int totalUpdated = 0;
		while (true) {
			List<Quote> quotes =
							quoteRepository.findPageByLanguageAndIdRange(lang, cursor, maxId, PAGE_SIZE);
			if (quotes.isEmpty()) break;

			for (Quote quote : quotes) {
				ensureProfile(quote);
				float seed = seedCalculator.calculate(quote.getProfile(), stats, lang);
				quote.updateDifficultySeed(seed);
			}
			totalUpdated += quotes.size();
			cursor = quotes.getLast().getId();
		}
		log.info("[{}] seed 재계산 완료 - {}건 갱신", lang, totalUpdated);
	}

	private void ensureProfile(Quote quote) {
		if (quote.getProfile() == null) {
			QuoteProfile profile =
							quoteProfileCalculator.calculate(quote.getSentence(), quote.getLanguage());
			quote.updateProfile(profile);
		}
	}

	/** rawSeed 의 모집단 평균·표준편차 누적기 (STDDEV_POP 과 동일 정의) */
	private static final class RawSeedAccumulator {
		private long count;
		private double sum;
		private double sumSq;

		void add(float value) {
			count++;
			sum += value;
			sumSq += (double) value * value;
		}

		float mean() {
			return (float) (sum / count);
		}

		float std() {
			double mean = sum / count;
			return (float) Math.sqrt(Math.max(0d, sumSq / count - mean * mean));
		}

		boolean isUsable() {
			return count >= 2 && std() > 0f;
		}
	}
}
