package com.typingpractice.typing_practice_be.word.service.difficulty;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.domain.WordProfile;
import com.typingpractice.typing_practice_be.word.statistics.domain.GlobalWordStatistics;
import org.springframework.stereotype.Component;

@Component
public class WordDifficultySeedCalculator {
  // 한국어 가중치
  private static final float W_LEN_KO = 0.30f;
  private static final float W_JAMO = 0.30f;
  private static final float W_DIPHTHONG = 0.20f;
  private static final float W_SHIFT_JAMO = 0.20f;

  // 영어 가중치
  private static final float W_LEN_EN = 0.60f;
  private static final float W_CASE = 0.40f;

  public float calculate(WordProfile profile, GlobalWordStatistics stats, WordLanguage language) {
    float sum = 0f;

    if (language == WordLanguage.KOREAN) {
      sum += W_LEN_KO * zScore(profile.getLength(), stats.getLenMean(), stats.getLenStd());
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
      sum += W_LEN_EN * zScore(profile.getLength(), stats.getLenMean(), stats.getLenStd());
      sum += W_CASE * zScore(profile.getCaseFlipRate(), stats.getCaseMean(), stats.getCaseStd());
    }

    return Math.round(100 * sum);
  }

  private float zScore(float value, float mean, float std) {
    if (std == 0f) return 0f;
    return clip((value - mean) / std);
  }

  private float clip(float value) {
    return Math.max(0f, Math.min(1f, (value + 2f) / 4f));
  }
}
