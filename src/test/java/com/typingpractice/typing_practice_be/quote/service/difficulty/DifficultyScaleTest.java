package com.typingpractice.typing_practice_be.quote.service.difficulty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DifficultyScaleTest {

  @Test
  @DisplayName("mapZ: 0 → 0.5, ±Z_RANGE → 1/0, 범위 밖은 클립")
  void mapZ() {
    assertThat(DifficultyScale.mapZ(0f)).isEqualTo(0.5f);
    assertThat(DifficultyScale.mapZ(DifficultyScale.Z_RANGE)).isEqualTo(1f);
    assertThat(DifficultyScale.mapZ(-DifficultyScale.Z_RANGE)).isEqualTo(0f);
    assertThat(DifficultyScale.mapZ(10f)).isEqualTo(1f);
    assertThat(DifficultyScale.mapZ(-10f)).isEqualTo(0f);
  }

  @Test
  @DisplayName("clampFeatureZ: ±2 로 자른다")
  void clampFeatureZ() {
    assertThat(DifficultyScale.clampFeatureZ(5f)).isEqualTo(2f);
    assertThat(DifficultyScale.clampFeatureZ(-5f)).isEqualTo(-2f);
    assertThat(DifficultyScale.clampFeatureZ(1.5f)).isEqualTo(1.5f);
  }

  @Test
  @DisplayName("standardize: (value - mean) / std")
  void standardize() {
    assertThat(DifficultyScale.standardize(0.3f, 0.1f, 0.2f)).isCloseTo(1f, within(1e-6f));
  }

  @Test
  @DisplayName("seed 기본값으로 매핑하면 현행 산식 100·(0.5 + raw/4) 와 같다")
  void seedDefaultMatchesLegacy() {
    for (float raw : new float[] {-1f, -0.3f, 0f, 0.3f, 1f}) {
      float mapped =
          DifficultyScale.mapZ(
              DifficultyScale.standardize(
                  raw, DifficultyScale.DEFAULT_SEED_MEAN, DifficultyScale.DEFAULT_SEED_STD));
      assertThat(mapped).isCloseTo(0.5f + raw / 4f, within(1e-5f));
    }
  }

  @Test
  @DisplayName("perf 기본값으로 매핑하면 현행 산식 (clip(1.5·raw, -1, 1) + 1)/2 와 같다")
  void perfDefaultMatchesLegacy() {
    for (float raw : new float[] {-0.5f, -0.2f, 0f, 0.2f, 0.5f}) {
      float mapped =
          DifficultyScale.mapZ(
              DifficultyScale.standardize(
                  raw, DifficultyScale.DEFAULT_PERF_MEAN, DifficultyScale.DEFAULT_PERF_STD));
      float legacy = (Math.max(-1f, Math.min(1f, 1.5f * raw)) + 1f) / 2f;
      assertThat(mapped).isCloseTo(legacy, within(1e-5f));
    }
  }
}
