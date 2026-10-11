package com.typingpractice.typing_practice_be.adaptiveserving.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.typingpractice.typing_practice_be.adaptiveserving.config.AdaptiveServingProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdaptiveServingCalculatorTest {
  // 설계서 4-1 값: beta 60, sigmaObs 15, perfCoefficient 1.5, defaultMu 50, defaultSigma 25, sigmaProcess 2, surprise 2
  private final AdaptiveServingCalculator calculator =
      new AdaptiveServingCalculator(new AdaptiveServingProperties(60f, 15f, 1.5f, 50f, 25f, 2f, 2f));

  /** perf 가 일정한 사용자를 n 회 갱신한 뒤의 (μ, σ) */
  private float[] run(float perf, int n) {
    float mu = 50f, sigma = 25f;
    for (int i = 0; i < n; i++) {
      float[] next = calculator.update(mu, sigma, calculator.calcObservation(perf));
      mu = next[0];
      sigma = next[1];
    }
    return new float[] {mu, sigma};
  }

  @Test
  @DisplayName("관측값은 코퍼스 중앙(default-mu) + β·perf 이고 문장 난이도와 무관하다")
  void observationIsAnchoredAtDefaultMu() {
    assertThat(calculator.calcObservation(0f)).isEqualTo(50f);
    assertThat(calculator.calcObservation(0.5f)).isEqualTo(80f);
    assertThat(calculator.calcObservation(-1f)).isEqualTo(-10f); // 클램프는 update 의 μ 에서
  }

  @Test
  @DisplayName("고정점: perf 0.3 이 일정하면 μ 는 50 + 60·0.3 = 68 에 수렴하고 그 뒤 움직이지 않는다 (드리프트 없음)")
  void convergesToFixedPointWithoutDrift() {
    float mu100 = run(0.3f, 100)[0];
    float mu200 = run(0.3f, 200)[0];

    assertThat(mu200).isCloseTo(68f, within(0.5f));
    assertThat(Math.abs(mu200 - mu100)).isLessThan(0.1f);
  }

  @Test
  @DisplayName("평균 사용자(perf 0)는 μ 50 을 유지하고 σ 는 단조 감소해 5.0~5.6 에 정착한다")
  void averageUserStaysAtCenter() {
    float mu = 50f, sigma = 25f;
    for (int i = 0; i < 100; i++) {
      float[] next = calculator.update(mu, sigma, calculator.calcObservation(0f));
      assertThat(next[1]).isLessThanOrEqualTo(sigma);
      mu = next[0];
      sigma = next[1];
    }
    assertThat(mu).isCloseTo(50f, within(0.01f));
    assertThat(sigma).isBetween(5.0f, 5.6f);
  }

  @Test
  @DisplayName("μ 는 항상 0~100 으로 잘린다 (perf ±1 → x 110 / -10)")
  void clampsMuToRange() {
    float mu = 50f, sigma = 25f;
    for (int i = 0; i < 100; i++) {
      float[] next = calculator.update(mu, sigma, calculator.calcObservation(1f));
      assertThat(next[0]).isLessThanOrEqualTo(100f);
      mu = next[0];
      sigma = next[1];
    }
    assertThat(mu).isEqualTo(100f);

    mu = 50f;
    sigma = 25f;
    for (int i = 0; i < 100; i++) {
      float[] next = calculator.update(mu, sigma, calculator.calcObservation(-1f));
      assertThat(next[0]).isGreaterThanOrEqualTo(0f);
      mu = next[0];
      sigma = next[1];
    }
    assertThat(mu).isEqualTo(0f);
  }

  @Test
  @DisplayName("칼만 수식 보존: μ 50, σ 25, x 80 → K 629/854, μ′ 72.10, σ′ 12.87")
  void kalmanFormulaUnchanged() {
    float[] next = calculator.update(50f, 25f, 80f);

    assertThat(next[0]).isCloseTo(72.10f, within(0.05f));
    assertThat(next[1]).isCloseTo(12.87f, within(0.05f));
  }

  @Test
  @DisplayName("surprise 적응 보존: σ 5, x 80 (surprise 6 > 2) → process noise 12 → K 169/394 → μ′ 62.87")
  void surpriseAdaptationUnchanged() {
    float[] next = calculator.update(50f, 5f, 80f);

    assertThat(next[0]).isCloseTo(62.87f, within(0.05f));
  }

  @Test
  @DisplayName("σ 가 0 이하로 들어오면 defaultSigma(25) 로 복구해 갱신한다")
  void zeroSigmaFallsBackToDefault() {
    float[] fromZero = calculator.update(50f, 0f, 80f);
    float[] fromDefault = calculator.update(50f, 25f, 80f);

    assertThat(fromZero[0]).isEqualTo(fromDefault[0]);
    assertThat(fromZero[1]).isEqualTo(fromDefault[1]);
  }

  @Test
  @DisplayName("perf 정의 불변: 10% 빠르고 정확도 같으면 0.15, 분모 0 이면 0")
  void perfNormalizedUnchanged() {
    assertThat(calculator.calcPerfNormalized(330f, 0.95f, 300f, 0.95f)).isCloseTo(0.15f, within(1e-5f));
    assertThat(calculator.calcPerfNormalized(300f, 0.95f, 0f, 0f)).isEqualTo(0f);
  }
}
