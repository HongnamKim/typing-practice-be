package com.typingpractice.typing_practice_be.quote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import com.typingpractice.typing_practice_be.quote.service.difficulty.DifficultySeedCalculator;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DifficultySeedCalculatorTest {
  private final DifficultySeedCalculator calculator = new DifficultySeedCalculator();

  // createKoreanDefault 의 평균값: len 30, punc 0.05, space 0.18, digit 0.0005, jamo 0.5, diphthong 0.08, shiftJamo 0.05
  private QuoteProfile koreanAverage() {
    return createKoreanProfile(30, 0.05f, 0.18f, 0.0005f, 0.5f, 0.08f, 0.05f);
  }

  // 길이만 평균 + 1σ (lenStd 10), 나머지는 평균 → rawSeed = W_LEN(0.20) × 1 = 0.2
  private QuoteProfile koreanLengthPlusOneSigma() {
    return createKoreanProfile(40, 0.05f, 0.18f, 0.0005f, 0.5f, 0.08f, 0.05f);
  }

  @Nested
  @DisplayName("rawSeed (표준화 전 가중합)")
  class Raw {
    @Test
    @DisplayName("평균 문장은 0")
    void averageIsZero() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();

      float raw = calculator.calculateRaw(koreanAverage(), stats, QuoteLanguage.KOREAN);

      assertThat(raw).isCloseTo(0f, within(1e-6f));
    }

    @Test
    @DisplayName("길이만 +1σ 이면 가중치 0.2")
    void lengthPlusOneSigma() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();

      float raw = calculator.calculateRaw(koreanLengthPlusOneSigma(), stats, QuoteLanguage.KOREAN);

      assertThat(raw).isCloseTo(0.2f, within(1e-6f));
    }

    @Test
    @DisplayName("특성 z 는 ±2 로 잘려 극단값 하나가 가중합을 지배하지 않는다 (길이 z=17 → 0.2 × 2)")
    void featureClamp() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      QuoteProfile profile = createKoreanProfile(200, 0.05f, 0.18f, 0.0005f, 0.5f, 0.08f, 0.05f);

      float raw = calculator.calculateRaw(profile, stats, QuoteLanguage.KOREAN);

      assertThat(raw).isCloseTo(0.4f, within(1e-6f));
    }

    @Test
    @DisplayName("σ = 0 인 특성은 z = 0 (중립)")
    void sigmaZeroIsNeutral() {
      GlobalQuoteStatistics zeroStats = createZeroStdStats();
      QuoteProfile profile = createKoreanProfile(50, 0.10f, 0.10f, 0.05f, 0.8f, 0.15f, 0.10f);

      float raw = calculator.calculateRaw(profile, zeroStats, QuoteLanguage.KOREAN);

      assertThat(raw).isEqualTo(0f);
    }
  }

  @Nested
  @DisplayName("한국어 seed")
  class KoreanSeed {
    @Test
    @DisplayName("평균 문장 → 50")
    void averageSentence() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();

      float seed = calculator.calculate(koreanAverage(), stats, QuoteLanguage.KOREAN);

      assertThat(seed).isEqualTo(50f);
    }

    @Test
    @DisplayName("통계 없을 때(기본값) 길이 +1σ → 55 (현행 산식 100·(0.5 + 0.2/4) 와 동일)")
    void defaultStdMatchesLegacy() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();

      float seed = calculator.calculate(koreanLengthPlusOneSigma(), stats, QuoteLanguage.KOREAN);

      assertThat(seed).isEqualTo(55f);
    }

    @Test
    @DisplayName("seedStd 가 작으면 같은 rawSeed 가 50 에서 더 멀어진다 (std 0.1 → z 2 → 90)")
    void standardizationWidens() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      stats.updateSeedDistribution(0f, 0.1f);

      float seed = calculator.calculate(koreanLengthPlusOneSigma(), stats, QuoteLanguage.KOREAN);

      assertThat(seed).isEqualTo(90f);
    }

    @Test
    @DisplayName("seedMean 이 양수면 같은 문장이 더 쉬운 쪽으로 이동한다 (mean 0.2 → 평균 문장 50 미만)")
    void meanShift() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      stats.updateSeedDistribution(0.2f, 0.8f);

      float seed = calculator.calculate(koreanAverage(), stats, QuoteLanguage.KOREAN);

      assertThat(seed).isEqualTo(45f);
    }

    @Test
    @DisplayName("모든 특성이 +2σ 이상이면 정확히 100 (NaN 없음), 쉬운 쪽 극단은 0 이상 15 미만")
    void range() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      // spaceRate 는 낮을수록 어려움 → 어려운 쪽은 0, 쉬운 쪽은 큰 값
      QuoteProfile hardest = createKoreanProfile(1000, 1.0f, 0.0f, 1.0f, 10.0f, 1.0f, 1.0f);
      // 비율 특성(punc, digit, diphthong, shiftJamo)은 0 아래로 내려갈 수 없어 평균이 작은 특성은
      // -2σ 에 못 미친다 (punc: (0-0.05)/0.03 = -1.67). rawSeed ≈ -1.57 → z -1.97 → 약 11
      QuoteProfile easiest = createKoreanProfile(0, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f);

      assertThat(calculator.calculate(hardest, stats, QuoteLanguage.KOREAN)).isEqualTo(100f);
      assertThat(calculator.calculate(easiest, stats, QuoteLanguage.KOREAN)).isBetween(0f, 15f);
    }

    @Test
    @DisplayName("σ = 0 통계로는 모든 문장이 50")
    void sigmaZeroGivesFifty() {
      GlobalQuoteStatistics zeroStats = createZeroStdStats();
      QuoteProfile profile = createKoreanProfile(50, 0.10f, 0.10f, 0.05f, 0.8f, 0.15f, 0.10f);

      float seed = calculator.calculate(profile, zeroStats, QuoteLanguage.KOREAN);

      assertThat(seed).isEqualTo(50f);
    }
  }

  @Nested
  @DisplayName("영어 seed")
  class EnglishSeed {
    // createEnglishDefault 의 평균값: len 40, punc 0.04, space 0.18, digit 0.01, case 0.05, wordLen 4.5
    @Test
    @DisplayName("평균 문장 → 50")
    void averageSentence() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createEnglishDefault();
      QuoteProfile profile = createEnglishProfile(40, 0.04f, 0.18f, 0.01f, 0.05f, 4.5f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.ENGLISH);

      assertThat(seed).isEqualTo(50f);
    }

    @Test
    @DisplayName("모든 값이 평균보다 높은 문장 → 50 초과")
    void hardSentence() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createEnglishDefault();
      QuoteProfile profile = createEnglishProfile(80, 0.10f, 0.08f, 0.05f, 0.15f, 8.0f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.ENGLISH);

      assertThat(seed).isGreaterThan(50f);
    }

    @Test
    @DisplayName("모든 특성이 +2σ 이상이면 100")
    void maxSeed() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createEnglishDefault();
      QuoteProfile profile = createEnglishProfile(1000, 1.0f, 0.0f, 1.0f, 1.0f, 100.0f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.ENGLISH);

      assertThat(seed).isEqualTo(100f);
    }
  }

  // === 헬퍼 메서드 ===

  private QuoteProfile createKoreanProfile(
      int length,
      float puncRate,
      float spaceRate,
      float digitRate,
      float jamoComplex,
      float diphthongRate,
      float shiftJamoRate) {
    QuoteProfile profile = QuoteProfile.create();
    profile.setLength(length);
    profile.setPuncRate(puncRate);
    profile.setSpaceRate(spaceRate);
    profile.setDigitRate(digitRate);
    profile.setJamoComplex(jamoComplex);
    profile.setDiphthongRate(diphthongRate);
    profile.setShiftJamoRate(shiftJamoRate);
    return profile;
  }

  private QuoteProfile createEnglishProfile(
      int length,
      float puncRate,
      float spaceRate,
      float digitRate,
      float caseFlipRate,
      float avgWordLen) {
    QuoteProfile profile = QuoteProfile.create();
    profile.setLength(length);
    profile.setPuncRate(puncRate);
    profile.setSpaceRate(spaceRate);
    profile.setDigitRate(digitRate);
    profile.setCaseFlipRate(caseFlipRate);
    profile.setAvgWordLen(avgWordLen);
    return profile;
  }

  /** 모든 특성 σ = 0 인 한국어 통계 (필드가 private 이라 리플렉션 사용) */
  private GlobalQuoteStatistics createZeroStdStats() {
    GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
    try {
      for (String field :
          new String[] {
            "lenStd", "puncStd", "spaceStd", "digitStd", "jamoStd", "diphthongStd", "shiftJamoStd"
          }) {
        java.lang.reflect.Field f = GlobalQuoteStatistics.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(stats, 0f);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return stats;
  }
}
