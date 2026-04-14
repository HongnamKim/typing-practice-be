package com.typingpractice.typing_practice_be.adaptiveserving.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdaptiveServingEstimation {
  private float mu;
  private float sigma;

  private AdaptiveServingEstimation(float mu, float sigma) {
    this.mu = mu;
    this.sigma = sigma;
  }

  public static AdaptiveServingEstimation of(float mu, float sigma) {
    return new AdaptiveServingEstimation(mu, sigma);
  }
}
