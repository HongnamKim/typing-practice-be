package com.typingpractice.typing_practice_be.quote.service.difficulty;

import com.typingpractice.typing_practice_be.quote.config.DifficultyProperties;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DifficultySeedCalculator {
  private final DifficultyProperties properties; // 가중치는 application.yaml quote.difficulty.seed-weights

  /**
   * 표준화 전 가중합 rawSeed = Σ wᵢ · zᵢ (zᵢ 는 ±2 클램프, σ = 0 이면 0). 평균 문장 = 0, 범위 [-2, 2].
   * 전역 통계 배치가 이 값의 코퍼스 평균·표준편차(seedMean/seedStd)를 구한다.
   */
  public float calculateRaw(
      QuoteProfile profile, GlobalQuoteStatistics stats, QuoteLanguage language) {
    float sum = 0f;

    if (language == QuoteLanguage.KOREAN) {
      DifficultyProperties.KoreanWeights w = properties.getSeedWeights().korean();
      sum += w.len() * zScore(profile.getLength(), stats.getLenMean(), stats.getLenStd());
      sum += w.punc() * zScore(profile.getPuncRate(), stats.getPuncMean(), stats.getPuncStd());
      sum +=
          w.space() * zScoreInverse(profile.getSpaceRate(), stats.getSpaceMean(), stats.getSpaceStd());
      sum += w.digit() * zScore(profile.getDigitRate(), stats.getDigitMean(), stats.getDigitStd());
      sum += w.jamo() * zScore(profile.getJamoComplex(), stats.getJamoMean(), stats.getJamoStd());
      sum +=
          w.diphthong()
              * zScore(profile.getDiphthongRate(), stats.getDiphthongMean(), stats.getDiphthongStd());
      sum +=
          w.shiftJamo()
              * zScore(profile.getShiftJamoRate(), stats.getShiftJamoMean(), stats.getShiftJamoStd());
    } else {
      DifficultyProperties.EnglishWeights w = properties.getSeedWeights().english();
      sum += w.len() * zScore(profile.getLength(), stats.getLenMean(), stats.getLenStd());
      sum += w.punc() * zScore(profile.getPuncRate(), stats.getPuncMean(), stats.getPuncStd());
      sum +=
          w.space() * zScoreInverse(profile.getSpaceRate(), stats.getSpaceMean(), stats.getSpaceStd());
      sum += w.digit() * zScore(profile.getDigitRate(), stats.getDigitMean(), stats.getDigitStd());
      sum += w.caseFlip() * zScore(profile.getCaseFlipRate(), stats.getCaseMean(), stats.getCaseStd());
      sum +=
          w.wordLen() * zScore(profile.getAvgWordLen(), stats.getWordLenMean(), stats.getWordLenStd());
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
