package com.typingpractice.typing_practice_be.quote.service.difficulty;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.quote.statistics.service.GlobalQuoteStatisticsService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
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
public class DifficultyBatchService {
  private final QuoteTypingStatsRepository quoteTypingStatsRepository; // 각 문장 별 타이핑 기록
  private final GlobalQuoteStatisticsService
      globalQuoteStatisticsService; // 일별 전체 문장 코퍼스, 속도, 정확도 기록
  private final DynamicDifficultyCalculator dynamicDifficultyCalculator; // 동적난이도 계산기

  private static final int PAGE_SIZE = 500;

  @Transactional
  public void runDynamicDifficultyBatch() {
    for (QuoteLanguage lang : QuoteLanguage.values()) {
      GlobalQuoteStatistics globalStats = globalQuoteStatisticsService.getByLanguage(lang);

      if (globalStats.getGlobalAvgCpm() == null || globalStats.getGlobalAvgAcc() == null) {
        log.info("[{}] 전역 타이핑 기록 없음 -> 동적 보정 스킵", lang);
        continue;
      }

      int totalUpdated = recalculateByLanguage(lang, globalStats);
      log.info("[{}] 동적 난이도 보정 완료 - {}건 갱신", lang, totalUpdated);
    }
  }

  private int recalculateByLanguage(QuoteLanguage lang, GlobalQuoteStatistics globalStats) {
    int totalUpdated = 0;
    int page = 0;

    while (true) {
      List<QuoteTypingStats> statsList =
          quoteTypingStatsRepository.findByLanguageWithQuote(lang, page, PAGE_SIZE);

      if (statsList.isEmpty()) break;

      for (QuoteTypingStats stats : statsList) {
        Quote quote = stats.getQuote();
        if (quote.getProfile() == null) {
          log.warn("[{}] Quote profile 미존재, 스킵 - quoteId: {}", lang, quote.getId());
          continue;
        }

        float seed = quote.getProfile().getDifficultySeed();

        float difficulty = dynamicDifficultyCalculator.calculate(stats, globalStats, seed);
        quote.updateDynamicDifficulty(difficulty);
      }

      totalUpdated += statsList.size();
      page++;
    }

    return totalUpdated;
  }
}
