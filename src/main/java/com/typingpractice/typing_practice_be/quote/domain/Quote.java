package com.typingpractice.typing_practice_be.quote.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

@Entity
@Getter
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE quote SET deleted = true, deleted_at = NOW() where quote_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quote extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "quote_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  private QuoteStatus status;

  @Enumerated(EnumType.STRING)
  private QuoteType type;

  @Enumerated(EnumType.STRING)
  private QuoteLanguage language;

  private Float difficulty;

  @Embedded private QuoteProfile profile;

  @OneToOne(mappedBy = "quote", fetch = FetchType.LAZY)
  private QuoteTypingStats typingStats;

  private String sentence;
  private String author;

  @Column(length = 64)
  private String sentenceHash; // 문장 완전일치 검증용

  private int reportCount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  public static final String DEFAULT_AUTHOR = "작자 미상";
  public static final int HIDDEN_THRESHOLD = 5; // 자동 숨김 기준값

  public static Quote create(
      Member member,
      String sentence,
      String author,
      QuoteType type,
      QuoteLanguage language,
      QuoteProfile profile,
      Float difficulty,
      String sentenceHash) {
    Quote quote = new Quote();
    quote.member = member;
    quote.sentenceHash = sentenceHash;
    quote.sentence = sentence;
    quote.author = author != null ? author : DEFAULT_AUTHOR;
    quote.type = type;
    quote.language = language;
    quote.profile = profile;
    quote.difficulty = difficulty;
    quote.reportCount = 0;
    quote.status = quote.type == QuoteType.PUBLIC ? QuoteStatus.PENDING : QuoteStatus.ACTIVE;

    return quote;
  }

  public void approvePublish() {
    this.type = QuoteType.PUBLIC;
    this.status = QuoteStatus.ACTIVE;
  }

  public void rejectPublish() {
    this.type = QuoteType.PRIVATE;
    this.status = QuoteStatus.ACTIVE;
  }

  public void update(String sentence, String author) {
    if (sentence != null) {
      this.sentence = sentence;
    }

    if (author != null) {
      this.author = StringUtils.hasText(author) ? author : DEFAULT_AUTHOR;
    }
  }

  public void updateSentenceHash(String sentenceHash) {
    this.sentenceHash = sentenceHash;
  }

  public void updateType(QuoteType quoteType) {
    this.type = quoteType;
  }

  public void updateProfile(QuoteProfile profile) {
    this.profile = profile;
  }

  public void updateDifficulty(float seed) {
    this.profile.setDifficultySeed(seed);
    this.difficulty = seed;
  }

  public void updateStatus(QuoteStatus quoteStatus) {
    this.status = quoteStatus;
  }

  public void increaseReportCount() {
    this.reportCount++;
  }

  public void decreaseReportCount() {
    this.reportCount--;
  }

  public void resetReportCount() {
    this.reportCount = 0;
  }

  public boolean shouldBeHidden() {
    return this.reportCount >= Quote.HIDDEN_THRESHOLD;
  }
}
