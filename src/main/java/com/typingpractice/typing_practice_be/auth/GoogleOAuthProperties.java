package com.typingpractice.typing_practice_be.auth;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google")
@Getter
public class GoogleOAuthProperties {
  private final String clientId;
  private final String clientSecret;
  private final String redirectUri;
  private final String tokenUri;
  private final String grantType;

  public GoogleOAuthProperties(
      String clientId, String clientSecret, String redirectUri, String tokenUri) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
    this.tokenUri = tokenUri;
    this.grantType = "authorization_code";
  }
}
