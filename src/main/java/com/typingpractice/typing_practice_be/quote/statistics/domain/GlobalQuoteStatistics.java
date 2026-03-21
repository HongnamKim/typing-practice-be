package com.typingpractice.typing_practice_be.quote.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@ToString
@SQLRestriction("deleted = false")
@SQLDelete(
    sql =
        "UPDATE global_quote_statistics set deleted = true, deleted_at = NOW() where global_quote_statistics_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GlobalQuoteStatistics extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "global_quote_statistics_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  private QuoteLanguage language; // KOREAN, ENGLISH

  // 타이핑 결과 전역 평균 (동적 보정용)
  private Float globalAvgCpm;
  private Float globalAvgAcc;

  // 공통
  private float lenMean;
  private float lenStd;

  private float puncMean;
  private float puncStd;

  private float spaceMean;
  private float spaceStd;

  private float digitMean;
  private float digitStd;

  // 한국어 전용 (ENGLISH 행은 null)
  private Float jamoMean;
  private Float jamoStd;

  private Float diphthongMean;
  private Float diphthongStd;

  private Float shiftJamoMean;
  private Float shiftJamoStd;

  // 영어 전용 (KOREAN 행은 null)
  private Float caseMean;
  private Float caseStd;

  private Float wordLenMean;
  private Float wordLenStd;

  public static GlobalQuoteStatistics createKoreanDefault() {
    GlobalQuoteStatistics stats = new GlobalQuoteStatistics();
    stats.language = QuoteLanguage.KOREAN;

    stats.globalAvgCpm = 300f;
    stats.globalAvgAcc = 0.95f;

    // 공통 초기값
    stats.lenMean = 30f;
    stats.lenStd = 10f;
    stats.puncMean = 0.05f;
    stats.puncStd = 0.03f;
    stats.spaceMean = 0.18f;
    stats.spaceStd = 0.05f;
    stats.digitMean = 0.0005f;
    stats.digitStd = 0.006f;

    // 한국어 전용
    stats.jamoMean = 0.5f;
    stats.jamoStd = 0.2f;
    stats.diphthongMean = 0.08f;
    stats.diphthongStd = 0.05f;
    stats.shiftJamoMean = 0.05f;
    stats.shiftJamoStd = 0.03f;

    return stats;
  }

  public static GlobalQuoteStatistics createEnglishDefault() {
    GlobalQuoteStatistics stats = new GlobalQuoteStatistics();
    stats.language = QuoteLanguage.ENGLISH;

    stats.globalAvgCpm = 300f;
    stats.globalAvgAcc = 0.95f;

    // 공통 초기값
    stats.lenMean = 40f;
    stats.lenStd = 15f;
    stats.puncMean = 0.04f;
    stats.puncStd = 0.02f;
    stats.spaceMean = 0.18f;
    stats.spaceStd = 0.04f;
    stats.digitMean = 0.01f;
    stats.digitStd = 0.02f;

    // 영어 전용
    stats.caseMean = 0.05f;
    stats.caseStd = 0.03f;
    stats.wordLenMean = 4.5f;
    stats.wordLenStd = 1.5f;

    return stats;
  }

  public static GlobalQuoteStatistics createFromAggregation(QuoteLanguage language, Object[] row) {
    GlobalQuoteStatistics stats = new GlobalQuoteStatistics();
    stats.language = language;

    stats.lenMean = toFloat(row[0]);
    stats.lenStd = toFloat(row[1]);
    stats.puncMean = toFloat(row[2]);
    stats.puncStd = toFloat(row[3]);
    stats.spaceMean = toFloat(row[4]);
    stats.spaceStd = toFloat(row[5]);
    stats.digitMean = toFloat(row[6]);
    stats.digitStd = toFloat(row[7]);

    if (language == QuoteLanguage.KOREAN) {
      stats.jamoMean = toFloat(row[8]);
      stats.jamoStd = toFloat(row[9]);
      stats.diphthongMean = toFloat(row[10]);
      stats.diphthongStd = toFloat(row[11]);
      stats.shiftJamoMean = toFloat(row[12]);
      stats.shiftJamoStd = toFloat(row[13]);
    } else {
      stats.caseMean = toFloat(row[14]);
      stats.caseStd = toFloat(row[15]);
      stats.wordLenMean = toFloat(row[16]);
      stats.wordLenStd = toFloat(row[17]);
    }

    return stats;
  }

  public void updateGlobalTypingPerformance(float globalAvgCpm, float globalAvgAcc) {
    this.globalAvgCpm = globalAvgCpm;
    this.globalAvgAcc = globalAvgAcc;
  }

  private static float toFloat(Object value) {
    if (value == null) return 0f;
    return ((Number) value).floatValue();
  }

  /*private static Float toFloatOrNull(Object value) {
    if (value == null) return null;
    return ((Number) value).floatValue();
  }*/
}
