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

  private int attemptsCount;
  private float avgCpm;
  private float avgAcc;
  private float avgResetCount;

  public static QuoteTypingStats create(
      Quote quote,
      QuoteLanguage language,
      int attemptsCount,
      float avgCpm,
      float avgAcc,
      float avgResetCount) {
    QuoteTypingStats stats = new QuoteTypingStats();
    stats.quote = quote;
    stats.language = language;
    stats.attemptsCount = attemptsCount;
    stats.avgCpm = avgCpm;
    stats.avgAcc = avgAcc;
    stats.avgResetCount = avgResetCount;

    return stats;
  }

  public void merge(int newCount, float newAvgCpm, float newAvgAcc, float newAvgResetCount) {
    int totalCount = this.attemptsCount + newCount;
    this.avgCpm = (this.avgCpm * this.attemptsCount + newAvgCpm * newCount) / totalCount;
    this.avgAcc = (this.avgAcc * this.attemptsCount + newAvgAcc * newCount) / totalCount;
    this.avgResetCount =
        (this.avgResetCount * this.attemptsCount + newAvgResetCount * newCount) / totalCount;
    this.attemptsCount = totalCount;
  }

  public void overwrite(int attemptsCount, float avgCpm, float avgAcc, float avgResetCount) {
    this.attemptsCount = attemptsCount;
    this.avgCpm = avgCpm;
    this.avgAcc = avgAcc;
    this.avgResetCount = avgResetCount;
  }
}
