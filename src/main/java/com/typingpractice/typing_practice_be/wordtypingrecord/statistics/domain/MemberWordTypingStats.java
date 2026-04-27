package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_member_word_typing_stats",
            columnNames = {"member_id", "language"}))
public class MemberWordTypingStats extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "member_word_typing_stats_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Enumerated(EnumType.STRING)
  private WordLanguage language;

  private int totalAttempts;
  private int totalWordsAttempted;
  private float avgWpm;
  private float avgAcc;
  private float bestWpm;
  private float totalPracticeTimeMin;

  private float estimatedDifficulty;
  private float estimatedUncertainty;

  @Column(columnDefinition = "timestamp with time zone")
  private LocalDateTime lastPracticedAt;

  public static MemberWordTypingStats create(
      Member member,
      WordLanguage language,
      int totalAttempts,
      int totalWordsAttempted,
      float avgWpm,
      float avgAcc,
      float bestWpm,
      float totalPracticeTimeMin,
      LocalDateTime lastPracticedAt) {
    MemberWordTypingStats stats = new MemberWordTypingStats();
    stats.member = member;
    stats.language = language;
    stats.totalAttempts = totalAttempts;
    stats.totalWordsAttempted = totalWordsAttempted;
    stats.avgWpm = avgWpm;
    stats.avgAcc = avgAcc;
    stats.bestWpm = bestWpm;
    stats.totalPracticeTimeMin = totalPracticeTimeMin;
    stats.lastPracticedAt = lastPracticedAt;
    return stats;
  }

  public void merge(
      int newAttempts,
      int newWordAttempted,
      float newAvgWpm,
      float newAvgAcc,
      float newBestWpm,
      float newPracticeTimeMin,
      LocalDateTime newLastPracticedAt) {
    int mergedAttempts = this.totalAttempts + newAttempts;
    if (mergedAttempts > 0) {
      this.avgWpm = (this.avgWpm * this.totalAttempts + newAvgWpm * newAttempts) / mergedAttempts;
      this.avgAcc = (this.avgAcc * this.totalAttempts + newAvgAcc * newAttempts) / mergedAttempts;
    }
    this.totalAttempts = mergedAttempts;
    this.totalWordsAttempted += newWordAttempted;
    this.bestWpm = Math.max(this.bestWpm, newBestWpm);
    this.totalPracticeTimeMin += newPracticeTimeMin;

    if (this.lastPracticedAt == null || newLastPracticedAt.isAfter(this.lastPracticedAt)) {
      this.lastPracticedAt = newLastPracticedAt;
    }
  }

  public void overwrite(
      int totalAttempts,
      int totalWordsAttempted,
      float avgWpm,
      float avgAcc,
      float bestWpm,
      float totalPracticeTimeMin,
      LocalDateTime lastPracticedAt) {
    this.totalAttempts = totalAttempts;
    this.totalWordsAttempted = totalWordsAttempted;
    this.avgWpm = avgWpm;
    this.avgAcc = avgAcc;
    this.bestWpm = bestWpm;
    this.totalPracticeTimeMin = totalPracticeTimeMin;
    this.lastPracticedAt = lastPracticedAt;
  }

  public void updateEstimation(float estimatedDifficulty, float estimatedUncertainty) {
    this.estimatedDifficulty = estimatedDifficulty;
    this.estimatedUncertainty = estimatedUncertainty;
  }
}
