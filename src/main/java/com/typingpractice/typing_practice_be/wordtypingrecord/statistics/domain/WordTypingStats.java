package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordTypingStats extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "word_typing_stats_id")
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "word_id", unique = true)
  private Word word;

  @Enumerated(EnumType.STRING)
  private WordLanguage language;

  private int totalAttemptsCount;
  private int validAttemptsCount;
  private float avgTimeMs;
  private float correctRate;

  public static WordTypingStats create(
      Word word,
      WordLanguage language,
      int totalAttemptsCount,
      int validAttemptsCount,
      float avgTimeMs,
      float correctRate) {
    WordTypingStats stats = new WordTypingStats();
    stats.word = word;
    stats.language = language;
    stats.totalAttemptsCount = totalAttemptsCount;
    stats.validAttemptsCount = validAttemptsCount;
    stats.avgTimeMs = avgTimeMs;
    stats.correctRate = correctRate;
    return stats;
  }

  public void merge(
      int newTotalCount, int newValidCount, float newAvgTimeMs, float newCorrectRate) {
    int mergedValidCount = this.validAttemptsCount + newValidCount;
    if (mergedValidCount > 0) {
      this.avgTimeMs =
          (this.avgTimeMs * this.validAttemptsCount + newAvgTimeMs * newValidCount)
              / mergedValidCount;
      this.correctRate =
          (this.correctRate * this.validAttemptsCount + newCorrectRate * newValidCount)
              / mergedValidCount;
    }
    this.totalAttemptsCount += newTotalCount;
    this.validAttemptsCount = mergedValidCount;
  }

  public void overwrite(
      int totalAttemptsCount, int validAttemptsCount, float avgTimeMs, float correctRate) {
    this.totalAttemptsCount = totalAttemptsCount;
    this.validAttemptsCount = validAttemptsCount;
    this.avgTimeMs = avgTimeMs;
    this.correctRate = correctRate;
  }
}
