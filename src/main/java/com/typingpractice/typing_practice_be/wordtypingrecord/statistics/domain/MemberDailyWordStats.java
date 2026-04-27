package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_member_daily_word_stats_member_date",
            columnNames = {"member_id", "language", "date"}))
public class MemberDailyWordStats extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "member_daily_word_stats_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Enumerated(EnumType.STRING)
  private WordLanguage language;

  private LocalDate date;

  private int attempts;
  private int wordsAttempted;
  private float avgWpm;
  private float avgAcc;
  private float bestWpm;
  private float practiceTimeMin;

  public static MemberDailyWordStats create(
      Member member,
      LocalDate date,
      WordLanguage language,
      int attempts,
      int wordsAttempted,
      float avgWpm,
      float avgAcc,
      float bestWpm,
      float practiceTimeMin) {
    MemberDailyWordStats stats = new MemberDailyWordStats();
    stats.member = member;
    stats.date = date;
    stats.language = language;
    stats.attempts = attempts;
    stats.wordsAttempted = wordsAttempted;
    stats.avgWpm = avgWpm;
    stats.avgAcc = avgAcc;
    stats.bestWpm = bestWpm;
    stats.practiceTimeMin = practiceTimeMin;
    return stats;
  }

  public void overwrite(
      int attempts,
      int wordsAttempted,
      float avgWpm,
      float avgAcc,
      float bestWpm,
      float practiceTimeMin) {
    this.attempts = attempts;
    this.wordsAttempted = wordsAttempted;
    this.avgWpm = avgWpm;
    this.avgAcc = avgAcc;
    this.bestWpm = bestWpm;
    this.practiceTimeMin = practiceTimeMin;
  }
}
