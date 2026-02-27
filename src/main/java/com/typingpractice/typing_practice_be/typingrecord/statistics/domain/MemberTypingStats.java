package com.typingpractice.typing_practice_be.typingrecord.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTypingStats extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "member_typing_stats_id")
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", unique = true)
  private Member member;

  private int totalAttempts;
  private float avgCpm;
  private float avgAcc;
  private int bestCpm;
  private float totalPracticeTimeMin;
  private int totalResetCount;

  @Column(columnDefinition = "timestamp with time zone")
  private LocalDateTime lastPracticedAt;

  public static MemberTypingStats create(
      Member member,
      int totalAttempts,
      float avgCpm,
      float avgAcc,
      int bestCpm,
      float totalPracticeTimeMin,
      int totalResetCount,
      LocalDateTime lastPracticedAt) {
    MemberTypingStats stats = new MemberTypingStats();
    stats.member = member;
    stats.totalAttempts = totalAttempts;
    stats.avgCpm = avgCpm;
    stats.avgAcc = avgAcc;
    stats.bestCpm = bestCpm;
    stats.totalPracticeTimeMin = totalPracticeTimeMin;
    stats.totalResetCount = totalResetCount;
    stats.lastPracticedAt = lastPracticedAt;
    return stats;
  }

  public void merge(
      int newAttempts,
      float newAvgCpm,
      float newAvgAcc,
      int newBestCpm,
      float newPracticeTimeMin,
      int newResetCount,
      LocalDateTime newLastPracticedAt) {
    int mergedAttempts = this.totalAttempts + newAttempts;

    this.avgCpm = (this.avgCpm * this.totalAttempts + newAvgCpm * newAttempts) / mergedAttempts;
    this.avgAcc = (this.avgAcc * this.totalAttempts + newAvgAcc * newAttempts) / mergedAttempts;
    this.totalAttempts = mergedAttempts;
    this.bestCpm = Math.max(this.bestCpm, newBestCpm);
    this.totalPracticeTimeMin += newPracticeTimeMin;
    this.totalResetCount += newResetCount;

    if (this.lastPracticedAt == null || newLastPracticedAt.isAfter(this.lastPracticedAt)) {
      this.lastPracticedAt = newLastPracticedAt;
    }
  }

  public void overwrite(
      int totalAttempts,
      float avgCpm,
      float avgAcc,
      int bestCpm,
      float totalPracticeTimeMin,
      int totalResetCount,
      LocalDateTime lastPracticedAt) {
    this.totalAttempts = totalAttempts;
    this.avgCpm = avgCpm;
    this.avgAcc = avgAcc;
    this.bestCpm = bestCpm;
    this.totalPracticeTimeMin = totalPracticeTimeMin;
    this.totalResetCount = totalResetCount;
    this.lastPracticedAt = lastPracticedAt;
  }
}
