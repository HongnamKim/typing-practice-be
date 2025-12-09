package com.typingpractice.typing_practice_be.quote.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = true)
  private Member member;

  public static Quote create(Member member, String sentence, String author, QuoteType type) {
    Quote quote = new Quote();
    quote.member = member;
    quote.sentence = sentence;
    quote.author = author;
    quote.type = type;
    quote.status = quote.type == QuoteType.PUBLIC ? QuoteStatus.PENDING : QuoteStatus.APPROVED;

    return quote;
  }

  public void updateStatus(QuoteStatus quoteStatus) {
    this.status = quoteStatus;
  }
}
