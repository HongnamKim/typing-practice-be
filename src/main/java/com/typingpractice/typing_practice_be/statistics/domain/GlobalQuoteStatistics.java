package com.typingpractice.typing_practice_be.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
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
    // stats.updatedAt = LocalDateTime.now();

    // 공통 초기값
    stats.lenMean = 30f;
    stats.lenStd = 10f;
    stats.puncMean = 0.05f;
    stats.puncStd = 0.03f;
    stats.spaceMean = 0.18f;
    stats.spaceStd = 0.05f;
    stats.digitMean = 0.01f;
    stats.digitStd = 0.02f;

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
    // stats.updatedAt = LocalDateTime.now();

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
}
