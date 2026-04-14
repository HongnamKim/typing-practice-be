package com.typingpractice.typing_practice_be.adaptiveserving.service;

import com.typingpractice.typing_practice_be.adaptiveserving.config.AdaptiveServingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdaptiveServingCalculator {
  private final AdaptiveServingProperties properties;

  /**
   * 해당 문장의 평균 대비 사용자의 실력을 -1 ~ +1 범위로 정규화
   *
   * @param userCpm 사용자 타자 속도
   * @param userAcc 사용자 정확도
   * @param quoteAvgCpm 문장의 평균 타자 속도
   * @param quoteAvgAcc 문장의 평균 정확도
   * @return 정규화 값
   */
  public float calcPerfNormalized(
      float userCpm, float userAcc, float quoteAvgCpm, float quoteAvgAcc) {
    if (quoteAvgCpm == 0 || quoteAvgAcc == 0) return 0f;

    float cpmGain = userCpm / quoteAvgCpm - 1;
    float accGain = userAcc / quoteAvgAcc - 1;

    return clip(properties.getPerfCoefficient() * (cpmGain + accGain), -1f, 1f);
  }

  /** 관측값 X 를 산출 이 사용자의 적정 난이도는 이 정도일 것이다라는 추정치 */
  public float calcObservation(float difficulty, float perfNormalized) {
    return difficulty + properties.getBeta() * perfNormalized;
  }

  public float[] update(float mu, float sigma, float x) {
    float sigmaObs = properties.getSigmaObs();
    float baseProcessNoise = properties.getSigmaProcess();
    float surpriseThreshold = properties.getSurpriseThreshold();

    // sigma 최소값 보장 (0 또는 음수 방지)
    if (sigma <= 0) sigma = properties.getDefaultSigma();
    if (sigmaObs <= 0) return new float[] {mu, sigma};

    // Adaptive process noise: surprise 가 크면 sigma 를 더 넓힘 -> 평소와 다른 데이터
    float surprise = Math.abs(x - mu) / sigma;
    float processNoise =
        surprise > surpriseThreshold ? baseProcessNoise * surprise : baseProcessNoise;

    // 칼만 필터: 상태 예측 (process noise 반영)
    float sigmaSq = sigma * sigma + processNoise * processNoise;

    // 칼만 필터: 관측 업데이트
    float sigmaObsSq = sigmaObs * sigmaObs;
    float K = sigmaSq / (sigmaSq + sigmaObsSq);
    float newMu = mu + K * (x - mu);
    float newSigmaSq = 1f / (1f / sigmaSq + 1f / sigmaObsSq);
    float newSigma = (float) Math.sqrt(newSigmaSq);

    return new float[] {newMu, newSigma};
  }

  private float clip(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }
}
