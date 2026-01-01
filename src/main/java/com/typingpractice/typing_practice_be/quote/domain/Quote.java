package com.typingpractice.typing_practice_be.quote.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
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

  private String sentence;
  private String author;

  private int reportCount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  //  @OneToMany(mappedBy = "quote", cascade = CascadeType.REMOVE)
  //  private List<Report> reports = new ArrayList<>();

  public static final String DEFAULT_AUTHOR = "작자 미상";
  public static final int HIDDEN_THRESHOLD = 5; // 자동 숨김 기준값

  public static Quote create(Member member, String sentence, String author, QuoteType type) {
    Quote quote = new Quote();
    quote.member = member;
    quote.sentence = sentence;
    quote.author = author != null ? author : DEFAULT_AUTHOR;
    quote.type = type;
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

  /*public void updateSentence(String sentence) {
    this.sentence = sentence;
  }*/

  /*public void updateAuthor(String author) {
    this.author = author;
  }*/

  public void updateType(QuoteType quoteType) {
    this.type = quoteType;
  }

  public void updateStatus(QuoteStatus quoteStatus) {
    this.status = quoteStatus;
  }

  public void increaseReportCount() {
    this.reportCount++;
  }

  public void resetReportCount() {
    this.reportCount = 0;
  }

  public boolean shouldBeHidden() {
    return this.reportCount >= Quote.HIDDEN_THRESHOLD;
  }
}
