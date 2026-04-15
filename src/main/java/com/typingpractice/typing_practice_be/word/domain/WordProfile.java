package com.typingpractice.typing_practice_be.word.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordProfile {
  private int length;
  private float difficultySeed;

  // 한국어 전용
  private Float jamoComplex;
  private Float diphthongRate;
  private Float shiftJamoRate;

  // 영어 전용
  private Float caseFlipRate;

  public static WordProfile create() {
    return new WordProfile();
  }
}
