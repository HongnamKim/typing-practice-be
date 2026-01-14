package com.typingpractice.typing_practice_be.auth.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenRotation {
  private String accessToken;
  private String refreshToken;

  public static TokenRotation create(String accessToken, String refreshToken) {
    TokenRotation tokenRotation = new TokenRotation();
    tokenRotation.accessToken = accessToken;
    tokenRotation.refreshToken = refreshToken;

    return tokenRotation;
  }
}
