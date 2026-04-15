package com.typingpractice.typing_practice_be.word.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.statistics.dto.WordProfileAggregation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GlobalWordStatistics extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "global_word_statistics_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  private WordLanguage language;

  // 공통
  private float lenMean;
  private float lenStd;

  // 한국어 전용
  private Float jamoMean;
  private Float jamoStd;
  private Float diphthongMean;
  private Float diphthongStd;
  private Float shiftJamoMean;
  private Float shiftJamoStd;

  // 영어 전용
  private Float caseMean;
  private Float caseStd;

  public static GlobalWordStatistics createKoreanDefault() {
    GlobalWordStatistics stats = new GlobalWordStatistics();
    stats.language = WordLanguage.KOREAN;
    stats.lenMean = 3f;
    stats.lenStd = 1.5f;
    stats.jamoMean = 0.5f;
    stats.jamoStd = 0.2f;
    stats.diphthongMean = 0.08f;
    stats.diphthongStd = 0.05f;
    stats.shiftJamoMean = 0.05f;
    stats.shiftJamoStd = 0.03f;
    return stats;
  }

  public static GlobalWordStatistics createEnglishDefault() {
    GlobalWordStatistics stats = new GlobalWordStatistics();
    stats.language = WordLanguage.ENGLISH;
    stats.lenMean = 6f;
    stats.lenStd = 2.5f;
    stats.caseMean = 0.05f;
    stats.caseStd = 0.03f;
    return stats;
  }

  public static GlobalWordStatistics createFromAggregation(
      WordLanguage language, WordProfileAggregation agg) {
    GlobalWordStatistics stats = new GlobalWordStatistics();
    stats.language = language;
    stats.lenMean = agg.getLenMean();
    stats.lenStd = agg.getLenStd();

    if (language == WordLanguage.KOREAN) {
      stats.jamoMean = agg.getJamoMean();
      stats.jamoStd = agg.getJamoStd();
      stats.diphthongMean = agg.getDiphthongMean();
      stats.diphthongStd = agg.getDiphthongStd();
      stats.shiftJamoMean = agg.getShiftJamoMean();
      stats.shiftJamoStd = agg.getShiftJamoStd();
    } else {
      stats.caseMean = agg.getCaseMean();
      stats.caseStd = agg.getCaseStd();
    }

    return stats;
  }
}
