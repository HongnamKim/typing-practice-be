package com.typingpractice.typing_practice_be.auth.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AuthTokenRefreshResponse {
  private String accessToken;
  private String refreshToken;

  public static AuthTokenRefreshResponse from(String accessToken, String refreshToken) {
    AuthTokenRefreshResponse response = new AuthTokenRefreshResponse();
    response.accessToken = accessToken;
    response.refreshToken = refreshToken;

    return response;
  }
}
