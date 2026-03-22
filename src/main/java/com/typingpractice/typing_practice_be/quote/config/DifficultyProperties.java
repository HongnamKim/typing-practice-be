package com.typingpractice.typing_practice_be.quote.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "quote.difficulty")
public class DifficultyProperties {
  private final int coldStartK;

  public DifficultyProperties(int coldStartK) {
    this.coldStartK = coldStartK;
  }
}
