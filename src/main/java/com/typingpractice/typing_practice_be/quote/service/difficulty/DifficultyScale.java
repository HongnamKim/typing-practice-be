package com.typingpractice.typing_practice_be.quote.service.difficulty;

/** 난이도 스케일 공통 상수·매핑. seed 와 동적 보정이 같은 매핑을 쓴다 (설계서 3-1). */
public final class DifficultyScale {
  /** ±Z_RANGE σ 를 0~100 에 대응시킨다 */
  public static final float Z_RANGE = 2.5f;

  /** 특성별 z 는 가중합 전에 ±2 로 자른다 (극단 특성 하나가 지배하지 않도록) */
  public static final float FEATURE_Z_CLAMP = 2f;

  // 통계가 없을 때의 기본값. 이 값이면 표준화 전 산식(seed: 100·(0.5 + raw/4), perf: 계수 1.5)과 동일하다.
  public static final float DEFAULT_SEED_MEAN = 0f;
  public static final float DEFAULT_SEED_STD = 2f / Z_RANGE;
  public static final float DEFAULT_PERF_MEAN = 0f;
  public static final float DEFAULT_PERF_STD = 2f / (3f * Z_RANGE);

  private DifficultyScale() {}

  public static float clampFeatureZ(float z) {
    return Math.max(-FEATURE_Z_CLAMP, Math.min(FEATURE_Z_CLAMP, z));
  }

  public static float standardize(float value, float mean, float std) {
    return (value - mean) / std;
  }

  public static float mapZ(float z) {
    return Math.max(0f, Math.min(1f, (z + Z_RANGE) / (2f * Z_RANGE)));
  }
}
