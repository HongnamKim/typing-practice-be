package com.typingpractice.typing_practice_be.typingrecord.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.QuoteTypingAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.QuoteTypingAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.QuoteTypingStatsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteTypingStatsBatchService {
  private final QuoteRepository quoteRepository;
  private final QuoteTypingAggregationRepository typingAggregationRepository;
  private final QuoteTypingStatsRepository quoteTypingStatsRepository;

  private static final int PAGE_SIZE = 1000;

  // 스케줄 배치: 전날 하루치 증분 집계
  @Transactional
  public void runScheduledBatch() {
    // 한국 기준 어제 날짜
    LocalDate yesterdayKst = LocalDate.now(TimeUtils.KST).minusDays(1);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayKst);

    log.info("QuoteTypingStats 증분 배치 시작 - 범위: {} ~ {}", from, to);
    int totalProcessed = processPages(from, to, false);
    log.info("QuoteTypingStats 증분 배치 완료 - {}건 처리", totalProcessed);
  }

  // 수동 재계산: 전체 덮어쓰기
  @Transactional
  public void runManualRecalculation() {
    log.info("QuoteTypingStats 전체 재계산 시작");
    int totalProcessed = processPages(null, null, true);
    log.info("QuoteTypingStats 전체 재계산 완료 - {}건 처리", totalProcessed);
  }

  private int processPages(LocalDateTime from, LocalDateTime to, boolean overwrite) {
    int totalProcessed = 0;

    for (QuoteLanguage language : QuoteLanguage.values()) {
      Long maxId = quoteRepository.findMaxIdByLanguage(language);
      if (maxId == null) continue; // 해당 언어 문장 없음

      Long cursor = 0L;

      while (true) {
        List<Quote> quotes =
            quoteRepository.findPageByLanguageAndIdRange(language, cursor, maxId, PAGE_SIZE);
        if (quotes.isEmpty()) break; // 전체 문장 처리 완료

        List<Long> quoteIds = quotes.stream().map(Quote::getId).toList();

        // 타이핑 기록 통계값
        List<QuoteTypingAggregation> aggregations =
            overwrite
                ? typingAggregationRepository.aggregateByQuoteIds(quoteIds)
                : typingAggregationRepository.aggregateByQuoteIdsBetween(quoteIds, from, to);

        Map<Long, Integer> totalAttemptsCount =
            overwrite
                ? typingAggregationRepository.countAllByQuoteIds(quoteIds)
                : typingAggregationRepository.countAllByQuoteIdsBetween(quoteIds, from, to);

        // {quoteId: 타이핑 통계값} map 변환
        Map<Long, QuoteTypingAggregation> aggMap =
            aggregations.stream()
                .collect(Collectors.toMap(QuoteTypingAggregation::getQuoteId, a -> a));

        // Quote 의 타이핑 통계 반영
        for (Quote quote : quotes) {
          QuoteTypingAggregation agg = aggMap.get(quote.getId());
          Integer totalAttemptCount = totalAttemptsCount.get(quote.getId());
          if (agg == null) continue; // 통계값이 없는 경우

          QuoteTypingStats stats =
              quoteTypingStatsRepository.findByQuoteId(quote.getId()).orElse(null);

          if (stats == null) {
            // 기존 타이핑 통계가 없으면 생성
            stats =
                QuoteTypingStats.create(
                    quote,
                    language,
                    // agg.getTotalAttemptsCount(),
                    totalAttemptCount,
                    agg.getValidAttemptsCount(),
                    (float) agg.getAvgCpm(),
                    (float) agg.getAvgAcc(),
                    (float) agg.getAvgResetCount());
            quoteTypingStatsRepository.save(stats);
          } else if (overwrite) {
            // 덮어쓰기
            stats.overwrite(
                // agg.getTotalAttemptsCount(),
                totalAttemptCount,
                agg.getValidAttemptsCount(),
                (float) agg.getAvgCpm(),
                (float) agg.getAvgAcc(),
                (float) agg.getAvgResetCount());
          } else {
            // 업데이트
            stats.merge(
                // agg.getTotalAttemptsCount(),
                totalAttemptCount,
                agg.getValidAttemptsCount(),
                (float) agg.getAvgCpm(),
                (float) agg.getAvgAcc(),
                (float) agg.getAvgResetCount());
          }

          totalProcessed++;
        }

        cursor = quotes.getLast().getId();
      }
    }

    return totalProcessed;
  }
}
