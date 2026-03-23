package com.typingpractice.typing_practice_be.typingrecord.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuoteTypingStats extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "quote_typing_stats_id")
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "quote_id", unique = true)
  private Quote quote;

  @Enumerated(EnumType.STRING)
  private QuoteLanguage language;

  private int totalAttemptsCount;
  private int validAttemptsCount;
  private float avgCpm;
  private float avgAcc;
  private float avgResetCount;

  public static QuoteTypingStats create(
      Quote quote,
      QuoteLanguage language,
      int totalAttemptsCount,
      int validAttemptsCount,
      float avgCpm,
      float avgAcc,
      float avgResetCount) {
    QuoteTypingStats stats = new QuoteTypingStats();
    stats.quote = quote;
    stats.language = language;
    stats.totalAttemptsCount = totalAttemptsCount;
    stats.validAttemptsCount = validAttemptsCount;
    stats.avgCpm = avgCpm;
    stats.avgAcc = avgAcc;
    stats.avgResetCount = avgResetCount;

    return stats;
  }

  public void merge(
      int newTotalCount,
      int newValidCount,
      float newAvgCpm,
      float newAvgAcc,
      float newAvgResetCount) {
    int mergedValidCount = this.validAttemptsCount + newValidCount;
    if (mergedValidCount > 0) {
      this.avgCpm =
          (this.avgCpm * this.validAttemptsCount + newAvgCpm * newValidCount) / mergedValidCount;
      this.avgAcc =
          (this.avgAcc * this.validAttemptsCount + newAvgAcc * newValidCount) / mergedValidCount;
      this.avgResetCount =
          (this.avgResetCount * this.validAttemptsCount + newAvgResetCount * newValidCount)
              / mergedValidCount;
    }
    this.totalAttemptsCount += newTotalCount;
    this.validAttemptsCount = mergedValidCount;
  }

  public void overwrite(
      int totalAttemptsCount,
      int validAttemptsCount,
      float avgCpm,
      float avgAcc,
      float avgResetCount) {
    this.totalAttemptsCount = totalAttemptsCount;
    this.validAttemptsCount = validAttemptsCount;
    this.avgCpm = avgCpm;
    this.avgAcc = avgAcc;
    this.avgResetCount = avgResetCount;
  }
}
