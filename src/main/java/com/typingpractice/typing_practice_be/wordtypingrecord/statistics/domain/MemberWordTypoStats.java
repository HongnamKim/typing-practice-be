package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
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
            name = "uq_member_word_typo_stats",
            columnNames = {"member_id", "language", "expected"}))
public class MemberWordTypoStats extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "member_word_typo_stats_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  @Enumerated(EnumType.STRING)
  private WordLanguage language;

  private String expected;
  private int typoCount;

  public static MemberWordTypoStats create(
      Member member, WordLanguage language, String expected, int count) {
    MemberWordTypoStats stats = new MemberWordTypoStats();
    stats.member = member;
    stats.language = language;
    stats.expected = expected;
    stats.typoCount = count;
    return stats;
  }

  public void merge(int count) {
    this.typoCount += count;
  }

  public void overwrite(int count) {
    this.typoCount = count;
  }
}
