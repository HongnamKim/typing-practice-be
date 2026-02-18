package com.typingpractice.typing_practice_be.statistics.service;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.statistics.repository.GlobalQuoteStatisticsRepository;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalQuoteStatisticsService {
  private Map<QuoteLanguage, GlobalQuoteStatistics> cache;
  private final GlobalQuoteStatisticsRepository globalQuoteStatisticsRepository;
  private final TransactionTemplate transactionTemplate;

  @PostConstruct
  public void init() {
    // DB 에서 전역 통계값 로드 -> cache 채움

    transactionTemplate.execute(
        status -> {
          if (globalQuoteStatisticsRepository.count() == 0) {
            globalQuoteStatisticsRepository.save(GlobalQuoteStatistics.createKoreanDefault());
            globalQuoteStatisticsRepository.save(GlobalQuoteStatistics.createEnglishDefault());
            log.info("전역 통계 초기값 생성");
          }

          cache =
              Arrays.stream(QuoteLanguage.values())
                  .map(globalQuoteStatisticsRepository::findTopByLanguageOrderByCreatedAtDesc)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .collect(
                      Collectors.toMap(GlobalQuoteStatistics::getLanguage, Function.identity()));

          log.info("전역 통계 캐시 로드 완료 — keys={}", cache.keySet());
          return null;
        });
  }

  public GlobalQuoteStatistics getByLanguage(QuoteLanguage language) {
    log.info(
        "getByLanguage 호출 - language={}, cache keys={}",
        language,
        cache != null ? cache.keySet() : "null");
    return cache.get(language);
  }

  public void refreshCache() {
    // 배치 완료 후 호출
    init();
  }
}
