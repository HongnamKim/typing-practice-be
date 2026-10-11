package com.typingpractice.typing_practice_be.quote.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "quote.difficulty")
public class DifficultyProperties {
  private static final float WEIGHT_SUM_TOLERANCE = 0.001f;

  private final int coldStartK;
  private final SeedWeights seedWeights; // 정적 seed 특성 가중치 (언어별, 합 1.0)

  public DifficultyProperties(int coldStartK, SeedWeights seedWeights) {
    this.coldStartK = coldStartK;
    this.seedWeights = seedWeights;
  }

  public record SeedWeights(KoreanWeights korean, EnglishWeights english) {}

  /** 한국어: 공통 4개 + 한국어 전용 3개. 띄어쓰기는 적을수록 어려움(계산기에서 부호 반전) */
  public record KoreanWeights(
      float len, float punc, float space, float digit, float jamo, float diphthong, float shiftJamo) {
    public KoreanWeights {
      requireSumOne("korean", len + punc + space + digit + jamo + diphthong + shiftJamo);
    }
  }

  /** 영어: 공통 4개 + 영어 전용 2개 */
  public record EnglishWeights(
      float len, float punc, float space, float digit, float caseFlip, float wordLen) {
    public EnglishWeights {
      requireSumOne("english", len + punc + space + digit + caseFlip + wordLen);
    }
  }

  private static void requireSumOne(String language, float sum) {
    if (Math.abs(sum - 1f) > WEIGHT_SUM_TOLERANCE) {
      throw new IllegalArgumentException(
          "quote.difficulty.seed-weights." + language + " 가중치 합은 1.0 이어야 합니다. 현재: " + sum);
    }
  }
}
