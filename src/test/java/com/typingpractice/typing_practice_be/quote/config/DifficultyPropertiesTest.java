package com.typingpractice.typing_practice_be.quote.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DifficultyPropertiesTest {

  @Test
  @DisplayName("언어별 seed 가중치를 보관하고 합이 1.0 이면 생성된다")
  void holdsWeights() {
    DifficultyProperties props = DifficultyPropertiesFixture.defaults();

    assertThat(props.getColdStartK()).isEqualTo(20);
    assertThat(props.getSeedWeights().korean().shiftJamo()).isEqualTo(0.35f);
    assertThat(props.getSeedWeights().korean().punc()).isEqualTo(0f);
    assertThat(props.getSeedWeights().english().caseFlip()).isEqualTo(0.25f);
  }

  @Test
  @DisplayName("한국어 가중치 합이 1.0 이 아니면 기동 시점에 실패한다")
  void rejectsKoreanWeightsNotSummingToOne() {
    assertThatThrownBy(
            () ->
                new DifficultyProperties.KoreanWeights(0.35f, 0.15f, 0.10f, 0.20f, 0f, 0f, 0.35f))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("korean");
  }

  @Test
  @DisplayName("영어 가중치 합이 1.0 이 아니면 기동 시점에 실패한다")
  void rejectsEnglishWeightsNotSummingToOne() {
    assertThatThrownBy(
            () -> new DifficultyProperties.EnglishWeights(0.20f, 0.15f, 0.10f, 0.15f, 0.25f, 0.30f))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("english");
  }

  @Test
  @DisplayName("부동소수 오차 범위(0.001) 안의 합은 허용한다")
  void toleratesFloatRounding() {
    DifficultyProperties.KoreanWeights w =
        new DifficultyProperties.KoreanWeights(0.3333f, 0.3333f, 0.3334f, 0f, 0f, 0f, 0f);

    assertThat(w.len()).isEqualTo(0.3333f);
  }
}
