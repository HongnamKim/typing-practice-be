package com.typingpractice.typing_practice_be.quote.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuoteProfile {
  // 공통
  private int length; // 길이
  private float puncRate; // 문장부호 비율
  private float spaceRate; // 띄어쓰기 비율
  private float digitRate; // 문장 내 숫자 비율

  private float difficultySeed; // 문장 고유 난이도

  // 한국어 전용 (영어 문장이면 null)
  private Float jamoComplex; // 받침 점수
  private Float diphthongRate; // 겹모음 ㅢ, ㅘ
  private Float shiftJamoRate; // 쌍자음, 특수모음

  // 영어 전용 (한국어 문장이면 null)
  private Float caseFlipRate; // 대소문자
  private Float avgWordLen; // 평균 단어 길이
}
