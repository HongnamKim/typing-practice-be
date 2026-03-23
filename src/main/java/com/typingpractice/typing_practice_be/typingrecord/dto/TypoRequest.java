package com.typingpractice.typing_practice_be.typingrecord.dto;

import com.typingpractice.typing_practice_be.typingrecord.domain.Typo;
import com.typingpractice.typing_practice_be.typingrecord.domain.TypoType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class TypoRequest {
  @NotNull private String expected;

  @NotNull private String actual;

  @NotNull private Integer position;

  @NotNull private TypoType type;

  public static TypoRequest create(
      String expected, String actual, Integer position, TypoType type) {
    TypoRequest request = new TypoRequest();

    request.expected = expected;
    request.actual = actual;
    request.position = position;
    request.type = type;

    return request;
  }

  public Typo toTypo() {
    return Typo.create(expected, actual, position, type);
  }
}
