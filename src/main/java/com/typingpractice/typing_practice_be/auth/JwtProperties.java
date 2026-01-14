package com.typingpractice.typing_practice_be.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
@Getter
@AllArgsConstructor
public class JwtProperties {
  private String secret;
  private long accessTokenExpirationMs;
  private long refreshTokenExpirationDays;
}
