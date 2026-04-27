package com.typingpractice.typing_practice_be.wordtypingrecord.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordTypo {
  private String expected;
  private String actual;
  private int position;
  private WordTypoType type;

  public static WordTypo create(String expected, String actual, int position, WordTypoType type) {
    WordTypo typo = new WordTypo();
    typo.expected = expected;
    typo.actual = actual;
    typo.position = position;
    typo.type = type;
    return typo;
  }
}
