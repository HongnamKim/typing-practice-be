package com.typingpractice.typing_practice_be.typingrecord.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
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
            name = "uq_member_daily_stats_member_date",
            columnNames = {"member_id", "date", "language"}))
public class MemberDailyStats extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "member_daily_stats_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Enumerated(EnumType.STRING)
  private QuoteLanguage language;

  private LocalDate date; // KST 날짜

  private int attempts;
  private float avgCpm;
  private float avgAcc;
  private int bestCpm;
  private int resetCount;
  private float practiceTimeMin;

  public static MemberDailyStats create(
      Member member,
      LocalDate date,
      QuoteLanguage language,
      int attempts,
      float avgCpm,
      float avgAcc,
      int bestCpm,
      int resetCount,
      float practiceTimeMin) {
    MemberDailyStats stats = new MemberDailyStats();

    stats.member = member;
    stats.date = date;
    stats.language = language;

    stats.attempts = attempts;
    stats.avgCpm = avgCpm;
    stats.avgAcc = avgAcc;
    stats.bestCpm = bestCpm;
    stats.resetCount = resetCount;
    stats.practiceTimeMin = practiceTimeMin;
    return stats;
  }

  public void overwrite(
      int attempts,
      float avgCpm,
      float avgAcc,
      int bestCpm,
      int resetCount,
      float practiceTimeMin) {
    this.attempts = attempts;
    this.avgCpm = avgCpm;
    this.avgAcc = avgAcc;
    this.bestCpm = bestCpm;
    this.resetCount = resetCount;
    this.practiceTimeMin = practiceTimeMin;
  }
}
