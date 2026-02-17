package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import com.typingpractice.typing_practice_be.statistics.domain.GlobalQuoteStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class DifficultySeedCalculatorTest {
  private final DifficultySeedCalculator calculator = new DifficultySeedCalculator();

  @Nested
  @DisplayName("한국어 난이도 계산")
  class KoreanDifficulty {

    @Test
    @DisplayName("평균과 동일한 문장 - 모든 z점수 0, seed 0")
    void averageSentence() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      QuoteProfile profile = createKoreanProfile(30, 0.05f, 0.18f, 0.01f, 0.5f, 0.08f, 0.05f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.KOREAN);

      assertThat(seed).isEqualTo(0f);
    }

    @Test
    @DisplayName("모든 값이 평균보다 높은 문장 - 높은 seed")
    void hardSentence() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      // 평균보다 1σ 이상 높은 값들
      QuoteProfile profile = createKoreanProfile(50, 0.10f, 0.08f, 0.05f, 0.8f, 0.15f, 0.10f);
      // spaceRate는 낮을수록 어려움 → 0.08은 평균(0.18)보다 낮으므로 어려움↑

      float seed = calculator.calculate(profile, stats, QuoteLanguage.KOREAN);

      assertThat(seed).isGreaterThan(50f);
    }

    @Test
    @DisplayName("모든 값이 평균보다 낮은 문장 - seed 0")
    void easySentence() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      QuoteProfile profile = createKoreanProfile(10, 0.01f, 0.30f, 0.0f, 0.1f, 0.01f, 0.01f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.KOREAN);

      assertThat(seed).isEqualTo(0f);
    }

    @Test
    @DisplayName("seed는 0~100 범위")
    void seedRange() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      // 극단적으로 높은 값
      QuoteProfile profile = createKoreanProfile(200, 0.50f, 0.01f, 0.30f, 1.5f, 0.50f, 0.50f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.KOREAN);

      assertThat(seed).isBetween(0f, 100f);
    }
  }

  @Nested
  @DisplayName("영어 난이도 계산")
  class EnglishDifficulty {

    @Test
    @DisplayName("평균과 동일한 문장 - seed 0")
    void averageSentence() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createEnglishDefault();
      QuoteProfile profile = createEnglishProfile(40, 0.04f, 0.18f, 0.01f, 0.05f, 4.5f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.ENGLISH);

      assertThat(seed).isEqualTo(0f);
    }

    @Test
    @DisplayName("모든 값이 평균보다 높은 문장 - 높은 seed")
    void hardSentence() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createEnglishDefault();
      QuoteProfile profile = createEnglishProfile(80, 0.10f, 0.08f, 0.05f, 0.15f, 8.0f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.ENGLISH);

      assertThat(seed).isGreaterThan(50f);
    }

    @Test
    @DisplayName("seed는 0~100 범위")
    void seedRange() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createEnglishDefault();
      QuoteProfile profile = createEnglishProfile(200, 0.50f, 0.01f, 0.30f, 0.50f, 15.0f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.ENGLISH);

      assertThat(seed).isBetween(0f, 100f);
    }
  }

  @Nested
  @DisplayName("σ = 0 처리")
  class SigmaZero {

    @Test
    @DisplayName("σ가 0인 항목의 score는 0")
    void sigmaZeroReturnsZero() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      // digitStd를 0으로 설정하면 score_digit = 0이 되어야 함
      // createKoreanDefault의 digitStd는 0.02f이므로 별도 stats가 필요

      // 모든 std를 0으로 설정한 통계
      GlobalQuoteStatistics zeroStats = createZeroStdStats(QuoteLanguage.KOREAN);
      QuoteProfile profile = createKoreanProfile(50, 0.10f, 0.10f, 0.05f, 0.8f, 0.15f, 0.10f);

      float seed = calculator.calculate(profile, zeroStats, QuoteLanguage.KOREAN);

      assertThat(seed).isEqualTo(0f);
    }
  }

  @Nested
  @DisplayName("가중치 합계 검증")
  class WeightValidation {

    @Test
    @DisplayName("한국어 - 모든 score가 1이면 seed = 100")
    void koreanMaxSeed() {
      GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
      // 모든 값을 평균 + 10σ 이상으로 설정 → 모든 score = 1.0
      QuoteProfile profile = createKoreanProfile(1000, 1.0f, 0.0f, 1.0f, 10.0f, 1.0f, 1.0f);

      float seed = calculator.calculate(profile, stats, QuoteLanguage.KOREAN);

      assertThat(seed).isEqualTo(100f);
    }

    @Test
    @DisplayName("영어 - 모든 score가 1이면 seed = 100")
    void englishMaxSeed() {
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

  private GlobalQuoteStatistics createZeroStdStats(QuoteLanguage language) {
    // 모든 std = 0인 통계 → 모든 score = 0
    // GlobalQuoteStatistics에 접근하려면 팩토리 메서드나 테스트용 생성 방법 필요
    // 현재 필드가 private이므로 리플렉션 사용
    GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
    try {
      setField(stats, "lenStd", 0f);
      setField(stats, "puncStd", 0f);
      setField(stats, "spaceStd", 0f);
      setField(stats, "digitStd", 0f);
      setField(stats, "jamoStd", 0f);
      setField(stats, "diphthongStd", 0f);
      setField(stats, "shiftJamoStd", 0f);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return stats;
  }

  private void setField(Object obj, String fieldName, Object value) throws Exception {
    java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
  }
}
