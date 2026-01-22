package com.typingpractice.typing_practice_be.auth.dto.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GoogleTokenResponse {
  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("expires_in")
  private Integer expiresIn;

  public static GoogleTokenResponse create(
      String accessToken, String tokenType, Integer expiresIn) {
    GoogleTokenResponse tokenResponse = new GoogleTokenResponse();
    tokenResponse.accessToken = accessToken;
    tokenResponse.tokenType = tokenType;
    tokenResponse.expiresIn = expiresIn;

    return tokenResponse;
  }
}
