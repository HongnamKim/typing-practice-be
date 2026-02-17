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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
          }

          cache =
              Arrays.stream(QuoteLanguage.values())
                  .map(globalQuoteStatisticsRepository::findTopByLanguageOrderByCreatedAtDesc)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .collect(
                      Collectors.toMap(GlobalQuoteStatistics::getLanguage, Function.identity()));

          return null;
        });
  }

  public GlobalQuoteStatistics getByLanguage(QuoteLanguage language) {
    return cache.get(language);
  }

  public void refreshCache() {
    // 배치 완료 후 호출
    init();
  }
}
