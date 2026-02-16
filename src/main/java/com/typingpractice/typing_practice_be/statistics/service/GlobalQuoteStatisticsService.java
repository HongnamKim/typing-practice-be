package com.typingpractice.typing_practice_be.statistics.service;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.statistics.repository.GlobalQuoteStatisticsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalQuoteStatisticsService {
  private Map<QuoteLanguage, GlobalQuoteStatistics> cache;
  private final GlobalQuoteStatisticsRepository globalQuoteStatisticsRepository;

  @PostConstruct
  public void init() {
    // DB 에서 전역 통계값 로드 -> cache 채움
    cache =
        globalQuoteStatisticsRepository.findAll().stream()
            .collect(Collectors.toMap(GlobalQuoteStatistics::getLanguage, Function.identity()));
  }

  public GlobalQuoteStatistics getByLanguage(QuoteLanguage language) {
    return cache.get(language);
  }

  public void refreshCache() {
    // 배치 완료 후 호출
    init();
  }
}
