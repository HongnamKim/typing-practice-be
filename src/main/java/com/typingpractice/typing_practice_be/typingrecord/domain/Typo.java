package com.typingpractice.typing_practice_be.typingrecord.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Typo {

  private String expected; // 정답
  private String actual; // 제출
  private int position; // 몇번째 글자
  private TypoType type; // INITIAL, MEDIAL, FINAL, LETTER

  public static Typo create(String expected, String actual, int position, TypoType type) {
    Typo typo = new Typo();
    typo.expected = expected;
    typo.actual = actual;
    typo.position = position;
    typo.type = type;

    return typo;
  }
}
