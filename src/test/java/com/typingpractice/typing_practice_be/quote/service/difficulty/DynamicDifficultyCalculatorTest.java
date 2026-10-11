package com.typingpractice.typing_practice_be.quote.service.difficulty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.typingpractice.typing_practice_be.quote.config.DifficultyProperties;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DynamicDifficultyCalculatorTest {
  private static final int K = 20;
  private static final int MANY = 1000; // n ≫ k → 동적 항 비중 1000/1020

  private final DynamicDifficultyCalculator calculator =
      new DynamicDifficultyCalculator(new DifficultyProperties(K));

  // createKoreanDefault: globalAvgCpm 300, globalAvgAcc 0.95
  private QuoteTypingStats stats(int validAttempts, float avgCpm, float avgAcc) {
    return QuoteTypingStats.create(
        null, QuoteLanguage.KOREAN, validAttempts, validAttempts, avgCpm, avgAcc, 0f);
  }

  @Test
  @DisplayName("전역 평균과 같은 성능 → perfNormalized 0.5 → 난이도 50")
  void averagePerformance() {
    GlobalQuoteStatistics global = GlobalQuoteStatistics.createKoreanDefault();

    float difficulty = calculator.calculate(stats(MANY, 300f, 0.95f), global, 50f);

    assertThat(difficulty).isCloseTo(50f, within(0.01f));
  }

  @Test
  @DisplayName("통계 없을 때(기본값) 10% 느린 문장 → 현행 산식(계수 1.5)과 동일: 57.353")
  void defaultMatchesLegacy() {
    GlobalQuoteStatistics global = GlobalQuoteStatistics.createKoreanDefault();
    // cpmDrop 0.1 → legacy perfNormalized (1.5·0.1 + 1)/2 = 0.575
    // difficulty = (1000/1020)·0.575 + (20/1020)·0.5 = 0.573529 → 57.353

    float difficulty = calculator.calculate(stats(MANY, 270f, 0.95f), global, 50f);

    assertThat(difficulty).isCloseTo(57.353f, within(0.01f));
  }

  @Test
  @DisplayName("perfStd 가 작으면 같은 10% 차이가 더 큰 난이도 차이로 나타난다 (std 0.05 → z 2 → 0.9 → 89.216)")
  void standardizationWidens() {
    GlobalQuoteStatistics global = GlobalQuoteStatistics.createKoreanDefault();
    global.updatePerfDistribution(0f, 0.05f);
    // mapZ(2) = 4.5/5 = 0.9 → (1000/1020)·0.9 + (20/1020)·0.5 = 0.892157 → 89.216

    float difficulty = calculator.calculate(stats(MANY, 270f, 0.95f), global, 50f);

    assertThat(difficulty).isCloseTo(89.216f, within(0.01f));
  }

  @Test
  @DisplayName("perfMean 을 빼므로 코퍼스 평균 perf 인 문장이 0.5 가 된다")
  void meanCentering() {
    GlobalQuoteStatistics global = GlobalQuoteStatistics.createKoreanDefault();
    global.updatePerfDistribution(0.1f, 0.05f); // 코퍼스 평균 perfRaw 가 0.1 인 상황

    float difficulty = calculator.calculate(stats(MANY, 270f, 0.95f), global, 50f);

    assertThat(difficulty).isCloseTo(50f, within(0.01f));
  }

  @Test
  @DisplayName("극단적으로 느린 문장은 1 로 클립된다 (cpmDrop 0.667, std 0.05 → z 13 → 1)")
  void clipAtOne() {
    GlobalQuoteStatistics global = GlobalQuoteStatistics.createKoreanDefault();
    global.updatePerfDistribution(0f, 0.05f);
    // (1000/1020)·1 + (20/1020)·0.5 = 0.990196 → 99.02

    float difficulty = calculator.calculate(stats(MANY, 100f, 0.95f), global, 50f);

    assertThat(difficulty).isCloseTo(99.02f, within(0.01f));
  }

  @Test
  @DisplayName("콜드스타트: 시도 0회면 seed 그대로")
  void coldStartUsesSeed() {
    GlobalQuoteStatistics global = GlobalQuoteStatistics.createKoreanDefault();
    global.updatePerfDistribution(0f, 0.05f);

    float difficulty = calculator.calculate(stats(0, 100f, 0.5f), global, 70f);

    assertThat(difficulty).isCloseTo(70f, within(0.001f));
  }
}
