package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import com.typingpractice.typing_practice_be.statistics.domain.GlobalQuoteStatistics;
import org.springframework.stereotype.Component;

@Component
public class DifficultySeedCalculator {
  public float calculate(
      QuoteProfile profile, GlobalQuoteStatistics stats, QuoteLanguage language) {
    // 공통 score 4개 + 언어 전용 score 3개
    // σ == 0이면 해당 score = 0
    // 가중합 → round(100 * sum) 반환

    return 0f;
  }
}
