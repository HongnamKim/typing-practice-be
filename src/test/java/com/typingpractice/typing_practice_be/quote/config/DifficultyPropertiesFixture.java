package com.typingpractice.typing_practice_be.quote.config;

/** 테스트용 DifficultyProperties 생성 — application.yaml 의 기본값과 동일 */
public final class DifficultyPropertiesFixture {
  public static final int COLD_START_K = 20;

  private DifficultyPropertiesFixture() {}

  public static DifficultyProperties.SeedWeights defaultSeedWeights() {
    return new DifficultyProperties.SeedWeights(
        new DifficultyProperties.KoreanWeights(0.35f, 0f, 0.10f, 0.20f, 0f, 0f, 0.35f),
        new DifficultyProperties.EnglishWeights(0.20f, 0.15f, 0.10f, 0.15f, 0.25f, 0.15f));
  }

  public static DifficultyProperties defaults() {
    return new DifficultyProperties(COLD_START_K, defaultSeedWeights());
  }
}
