package com.typingpractice.typing_practice_be.word.statistics.service;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.statistics.domain.GlobalWordStatistics;
import com.typingpractice.typing_practice_be.word.statistics.repository.GlobalWordStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GlobalWordStatisticsService {
  private final GlobalWordStatisticsRepository repository;

  @Transactional
  public GlobalWordStatistics findByLanguage(WordLanguage language) {
    return repository
        .findByLanguage(language)
        .orElseGet(
            () -> {
              GlobalWordStatistics stats =
                  language == WordLanguage.KOREAN
                      ? GlobalWordStatistics.createKoreanDefault()
                      : GlobalWordStatistics.createEnglishDefault();
              repository.save(stats);
              return stats;
            });
  }
}
