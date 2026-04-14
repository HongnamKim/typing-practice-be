package com.typingpractice.typing_practice_be.quote.service.difficulty;

import com.typingpractice.typing_practice_be.quote.config.DifficultyProperties;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DynamicDifficultyCalculator {
  private static final float PERF_COEFFICIENT = 1.5f;

  private final DifficultyProperties difficultyProperties;

  public float calculate(
      QuoteTypingStats stats, GlobalQuoteStatistics globalStats, float difficultySeed) {
    int n = stats.getValidAttemptsCount();
    int k = difficultyProperties.getColdStartK();

    float cpmDrop = 1 - stats.getAvgCpm() / globalStats.getGlobalAvgCpm();
    float accDrop = 1 - stats.getAvgAcc() / globalStats.getGlobalAvgAcc();

    float perf = clip(PERF_COEFFICIENT * (cpmDrop + accDrop), -1f, 1f);
    float perfNormalized = (perf + 1f) / 2f;

    float difficulty =
        ((float) n / (n + k)) * perfNormalized + ((float) k / (n + k)) * (difficultySeed / 100f);

    return Math.round(100_000 * difficulty) / 1000f;
  }

  private float clip(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }
}
