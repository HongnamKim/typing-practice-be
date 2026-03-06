package com.typingpractice.typing_practice_be.typingrecord.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_member_typo_detail_stats",
            columnNames = {"member_id", "language", "expected", "actual"}))
public class MemberTypoDetailStats extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "member_typo_detail_stats_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Enumerated(EnumType.STRING)
  private QuoteLanguage language;

  private String expected;
  private String actual;
  private int count;
  private int initialCount;
  private int medialCount;
  private int finalCount;
  private int letterCount;

  public static MemberTypoDetailStats create(
      Member member,
      QuoteLanguage language,
      String expected,
      String actual,
      int count,
      int initialCount,
      int medialCount,
      int finalCount,
      int letterCount) {
    MemberTypoDetailStats stats = new MemberTypoDetailStats();
    stats.member = member;
    stats.language = language;
    stats.expected = expected;
    stats.actual = actual;
    stats.count = count;
    stats.initialCount = initialCount;
    stats.medialCount = medialCount;
    stats.finalCount = finalCount;
    stats.letterCount = letterCount;
    return stats;
  }

  public void merge(int count, int initialCount, int medialCount, int finalCount, int letterCount) {
    this.count += count;
    this.initialCount += initialCount;
    this.medialCount += medialCount;
    this.finalCount += finalCount;
    this.letterCount += letterCount;
  }

  public void overwrite(
      int count, int initialCount, int medialCount, int finalCount, int letterCount) {
    this.count = count;
    this.initialCount = initialCount;
    this.medialCount = medialCount;
    this.finalCount = finalCount;
    this.letterCount = letterCount;
  }
}
