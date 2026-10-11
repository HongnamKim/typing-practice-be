package com.typingpractice.typing_practice_be.quote.service.difficulty;

import com.typingpractice.typing_practice_be.quote.config.DifficultyProperties;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DynamicDifficultyCalculator {
  private final DifficultyProperties difficultyProperties;

  /** 설계서 3-3: perfRaw 를 코퍼스 기준으로 표준화한 뒤 mapZ 로 매핑, seed 와 n/(n+k) 블렌딩 */
  public float calculate(
      QuoteTypingStats stats, GlobalQuoteStatistics globalStats, float difficultySeed) {
    int n = stats.getValidAttemptsCount();
    int k = difficultyProperties.getColdStartK();

    float cpmDrop = 1 - stats.getAvgCpm() / globalStats.getGlobalAvgCpm();
    float accDrop = 1 - stats.getAvgAcc() / globalStats.getGlobalAvgAcc();
    float perfRaw = cpmDrop + accDrop;

    float zPerf =
        DifficultyScale.standardize(
            perfRaw, globalStats.getPerfMeanOrDefault(), globalStats.getPerfStdOrDefault());
    float perfNormalized = DifficultyScale.mapZ(zPerf);

    float difficulty =
        ((float) n / (n + k)) * perfNormalized + ((float) k / (n + k)) * (difficultySeed / 100f);

    return Math.round(100_000 * difficulty) / 1000f;
  }
}
