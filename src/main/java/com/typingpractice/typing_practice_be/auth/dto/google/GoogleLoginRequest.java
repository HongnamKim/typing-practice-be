package com.typingpractice.typing_practice_be.auth.dto.google;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GoogleLoginRequest {
  private String code;

  private String redirectUri;

  public static GoogleLoginRequest create(String code, @Nullable String redirectUri) {
    GoogleLoginRequest request = new GoogleLoginRequest();
    request.code = code;

    request.redirectUri = redirectUri;

    return request;
  }
}
