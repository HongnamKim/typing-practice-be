package com.typingpractice.typing_practice_be.quote.config;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quote.similarity-threshold")
public class SimilarityThresholdProperties {
  private final float korean;
  private final float english;

  public SimilarityThresholdProperties(float korean, float english) {
    this.korean = korean;
    this.english = english;
  }

  public float getByLanguage(QuoteLanguage language) {
    return language == QuoteLanguage.KOREAN ? korean : english;
  }
}
