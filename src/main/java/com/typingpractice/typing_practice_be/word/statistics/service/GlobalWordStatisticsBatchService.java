package com.typingpractice.typing_practice_be.word.statistics.service;

import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.domain.WordProfile;
import com.typingpractice.typing_practice_be.word.repository.WordRepository;
import com.typingpractice.typing_practice_be.word.service.WordIdCacheService;
import com.typingpractice.typing_practice_be.word.service.difficulty.WordDifficultySeedCalculator;
import com.typingpractice.typing_practice_be.word.service.difficulty.WordProfileCalculator;
import com.typingpractice.typing_practice_be.word.statistics.domain.GlobalWordStatistics;
import com.typingpractice.typing_practice_be.word.statistics.dto.WordProfileAggregation;
import com.typingpractice.typing_practice_be.word.statistics.repository.GlobalWordStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalWordStatisticsBatchService {
  private final GlobalWordStatisticsRepository statsRepository;
  private final WordIdCacheService wordIdCacheService;

  private final WordRepository wordRepository;
  private final WordDifficultySeedCalculator seedCalculator;
  private final WordProfileCalculator profileCalculator;

  private static final int PAGE_SIZE = 500;

  @Transactional
  public void runScheduledBatch() {
    for (WordLanguage lang : WordLanguage.values()) {
      GlobalWordStatistics next = recalculateStats(lang);

      recalculateAllSeeds(lang, next);
    }
  }

  @Transactional
  public void runManualRecalculation() {
    for (WordLanguage lang : WordLanguage.values()) {
      GlobalWordStatistics next = recalculateStats(lang);
      log.info("[Word:{}] 수동 트리거 -> seed 재계산 시작", lang);
      recalculateAllSeeds(lang, next);
    }
  }

  private GlobalWordStatistics recalculateStats(WordLanguage lang) {
    WordProfileAggregation agg = statsRepository.aggregateByLanguage(lang);
    GlobalWordStatistics next = GlobalWordStatistics.createFromAggregation(lang, agg);
    statsRepository.save(next);
    log.info("[Word:{}] 전역 통계 재계산 완료 - lenMean={}", lang, next.getLenMean());
    return next;
  }

  private void recalculateAllSeeds(WordLanguage lang, GlobalWordStatistics stats) {
    Long maxId = wordRepository.findMaxIdByLanguage(lang);
    if (maxId == null) return;

    Long cursor = 0L;
    int totalUpdated = 0;

    while (true) {
      List<Word> words =
          wordRepository.findPageByLanguageAndIdRange(lang, cursor, maxId, PAGE_SIZE);
      if (words.isEmpty()) break;

      for (Word word : words) {
        if (word.getProfile() == null) {
          WordProfile profile = profileCalculator.calculate(word.getWord(), word.getLanguage());
          word.updateProfile(profile);
        }

        float seed = seedCalculator.calculate(word.getProfile(), stats, lang);
        word.updateDifficultySeed(seed);
      }

      totalUpdated += words.size();
      cursor = words.getLast().getId();
    }

    wordIdCacheService.buildCache(lang);

    log.info("[Word:{}] seed 재계산 완료 - {}건 갱신", lang, totalUpdated);
  }
}
