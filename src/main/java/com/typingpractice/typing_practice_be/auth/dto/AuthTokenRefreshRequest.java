package com.typingpractice.typing_practice_be.auth.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AuthTokenRefreshRequest {
  private String refreshToken;

  public static AuthTokenRefreshRequest create(String refreshToken) {
    AuthTokenRefreshRequest request = new AuthTokenRefreshRequest();
    request.refreshToken = refreshToken;

    return request;
  }
}
