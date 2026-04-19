package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.repository.WordRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.query.aggregation.WordTypingAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.WordTypingStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.WordTypingAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.WordTypingStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordTypingStatsBatchService {
  private final WordRepository wordRepository;
  private final WordTypingAggregationRepository typingAggregationRepository;
  private final WordTypingStatsRepository wordTypingStatsRepository;

  private static final int PAGE_SIZE = 1000;

  @Transactional
  public void runScheduledBatch() {
    LocalDate yesterdayKst = LocalDate.now(TimeUtils.KST).minusDays(1);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayKst);

    log.info("WordTypingStats 증분 배치 시작 - 범위: {} ~ {}", from, to);
    int totalProcessed = processPages(from, to, false);
    log.info("WordTypingStats 증분 배치 완료 - {}건 처리", totalProcessed);
  }

  @Transactional
  public void runManualRecalculation() {
    log.info("WordTypingStats 전체 재계산 시작");
    int totalProcessed = processPages(null, null, true);
    log.info("WordTypingStats 전체 재계산 완료 - {}건 처리", totalProcessed);
  }

  private int processPages(LocalDateTime from, LocalDateTime to, boolean overwrite) {
    int totalProcessed = 0;

    for (WordLanguage language : WordLanguage.values()) {
      Long maxId = wordRepository.findMaxIdByLanguage(language);
      if (maxId == null) continue;

      Long cursor = 0L;

      while (true) {
        List<Word> words =
            wordRepository.findPageByLanguageAndIdRange(language, cursor, maxId, PAGE_SIZE);
        if (words.isEmpty()) break;

        List<Long> wordIds = words.stream().map(Word::getId).toList();

        List<WordTypingAggregation> aggregations =
            overwrite
                ? typingAggregationRepository.aggregateByWordIds(wordIds)
                : typingAggregationRepository.aggregateByWordIdsBetween(wordIds, from, to);

        Map<Long, Integer> totalAttemptsCount =
            overwrite
                ? typingAggregationRepository.countAllByWordIds(wordIds)
                : typingAggregationRepository.countAllByWordIdsBetween(wordIds, from, to);

        Map<Long, WordTypingAggregation> aggMap =
            aggregations.stream()
                .collect(Collectors.toMap(WordTypingAggregation::getWordId, a -> a));

        for (Word word : words) {
          WordTypingAggregation agg = aggMap.get(word.getId());
          Integer totalCount = totalAttemptsCount.get(word.getId());
          if (agg == null) continue;

          WordTypingStats stats = wordTypingStatsRepository.findByWordId(word.getId()).orElse(null);

          if (stats == null) {
            stats =
                WordTypingStats.create(
                    word,
                    language,
                    totalCount != null ? totalCount : agg.getValidAttemptsCount(),
                    agg.getValidAttemptsCount(),
                    agg.getAvgTimeMs(),
                    agg.getCorrectRate());
            wordTypingStatsRepository.save(stats);
          } else if (overwrite) {
            stats.overwrite(
                totalCount != null ? totalCount : agg.getValidAttemptsCount(),
                agg.getValidAttemptsCount(),
                agg.getAvgTimeMs(),
                agg.getCorrectRate());
          } else {
            stats.merge(
                totalCount != null ? totalCount : agg.getValidAttemptsCount(),
                agg.getValidAttemptsCount(),
                agg.getAvgTimeMs(),
                agg.getCorrectRate());
          }

          totalProcessed++;
        }

        cursor = words.getLast().getId();
      }
    }

    return totalProcessed;
  }
}
