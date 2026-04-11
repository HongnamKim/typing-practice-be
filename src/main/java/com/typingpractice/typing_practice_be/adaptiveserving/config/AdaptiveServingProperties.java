package com.typingpractice.typing_practice_be.adaptiveserving.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "adaptive-serving")
public class AdaptiveServingProperties {
  private final float beta;
  private final float sigmaObs;
  private final float perfCoefficient;
  private final float defaultMu;
  private final float defaultSigma;
  private final float sigmaProcess;
  private final float surpriseThreshold;

  public AdaptiveServingProperties(
      float beta,
      float sigmaObs,
      float perfCoefficient,
      float defaultMu,
      float defaultSigma,
      float sigmaProcess,
      float surpriseThreshold) {
    this.beta = beta;
    this.sigmaObs = sigmaObs;
    this.perfCoefficient = perfCoefficient;
    this.defaultMu = defaultMu;
    this.defaultSigma = defaultSigma;
    this.sigmaProcess = sigmaProcess;
    this.surpriseThreshold = surpriseThreshold;
  }
}
