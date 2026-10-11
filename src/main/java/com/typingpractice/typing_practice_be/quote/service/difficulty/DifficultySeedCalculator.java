package com.typingpractice.typing_practice_be.quote.service.difficulty;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import org.springframework.stereotype.Component;

@Component
public class DifficultySeedCalculator {
  // 공통 가중치
  private static final float W_LEN = 0.20f;
  private static final float W_PUNC = 0.15f;
  private static final float W_SPACE = 0.10f;
  private static final float W_DIGIT = 0.15f;

  // 한국어 전용 가중치
  private static final float W_JAMO = 0.15f;
  private static final float W_DIPHTHONG = 0.10f;
  private static final float W_SHIFT_JAMO = 0.15f;

  // 영어 전용 가중치
  private static final float W_CASE = 0.25f;
  private static final float W_WORD_LEN = 0.15f;

  /**
   * 표준화 전 가중합 rawSeed = Σ wᵢ · zᵢ (zᵢ 는 ±2 클램프, σ = 0 이면 0). 평균 문장 = 0, 범위 [-2, 2].
   * 전역 통계 배치가 이 값의 코퍼스 평균·표준편차(seedMean/seedStd)를 구한다.
   */
  public float calculateRaw(
      QuoteProfile profile, GlobalQuoteStatistics stats, QuoteLanguage language) {
    float sum = 0f;

    // 공통
    sum += W_LEN * zScore(profile.getLength(), stats.getLenMean(), stats.getLenStd());
    sum += W_PUNC * zScore(profile.getPuncRate(), stats.getPuncMean(), stats.getPuncStd());
    sum +=
        W_SPACE * zScoreInverse(profile.getSpaceRate(), stats.getSpaceMean(), stats.getSpaceStd());
    sum += W_DIGIT * zScore(profile.getDigitRate(), stats.getDigitMean(), stats.getDigitStd());

    if (language == QuoteLanguage.KOREAN) {
      sum += W_JAMO * zScore(profile.getJamoComplex(), stats.getJamoMean(), stats.getJamoStd());
      sum +=
          W_DIPHTHONG
              * zScore(
                  profile.getDiphthongRate(), stats.getDiphthongMean(), stats.getDiphthongStd());
      sum +=
          W_SHIFT_JAMO
              * zScore(
                  profile.getShiftJamoRate(), stats.getShiftJamoMean(), stats.getShiftJamoStd());
    } else {
      sum += W_CASE * zScore(profile.getCaseFlipRate(), stats.getCaseMean(), stats.getCaseStd());
      sum +=
          W_WORD_LEN
              * zScore(profile.getAvgWordLen(), stats.getWordLenMean(), stats.getWordLenStd());
    }

    return sum;
  }

  /** rawSeed 를 코퍼스 기준으로 표준화한 뒤 0~100 으로 매핑 (설계서 3-2) */
  public float calculate(QuoteProfile profile, GlobalQuoteStatistics stats, QuoteLanguage language) {
    float raw = calculateRaw(profile, stats, language);
    float z =
        DifficultyScale.standardize(raw, stats.getSeedMeanOrDefault(), stats.getSeedStdOrDefault());
    return Math.round(100 * DifficultyScale.mapZ(z));
  }

  private float zScore(float value, float mean, float std) {
    if (std == 0f) return 0f;
    return DifficultyScale.clampFeatureZ((value - mean) / std);
  }

  private float zScoreInverse(float value, float mean, float std) {
    if (std == 0f) return 0f;
    return DifficultyScale.clampFeatureZ((mean - value) / std);
  }
}
