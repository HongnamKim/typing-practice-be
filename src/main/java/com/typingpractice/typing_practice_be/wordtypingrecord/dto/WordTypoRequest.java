package com.typingpractice.typing_practice_be.wordtypingrecord.dto;

import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypo;
import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypoType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class WordTypoRequest {
  @NotNull private String expected;
  @NotNull private String actual;
  private int position;
  @NotNull private WordTypoType type;

  public WordTypo toWordTypo() {
    return WordTypo.create(expected, actual, position, type);
  }
}
