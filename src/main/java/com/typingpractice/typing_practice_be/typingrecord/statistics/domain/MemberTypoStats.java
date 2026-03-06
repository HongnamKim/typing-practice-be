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
            name = "uq_member_typo_stats",
            columnNames = {"member_id", "language", "expected"}))
public class MemberTypoStats extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "member_typo_stats_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Enumerated(EnumType.STRING)
  private QuoteLanguage language;

  private String expected;
  private int count;

  public static MemberTypoStats create(
      Member member, QuoteLanguage language, String expected, int count) {
    MemberTypoStats stats = new MemberTypoStats();
    stats.member = member;
    stats.language = language;
    stats.expected = expected;
    stats.count = count;
    return stats;
  }

  public void merge(int count) {
    this.count += count;
  }

  public void overwrite(int count) {
    this.count = count;
  }
}
