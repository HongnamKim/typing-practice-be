package com.typingpractice.typing_practice_be.quote.domain;

import com.typingpractice.typing_practice_be.member.domain.Member;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE quote SET deleted = true, delted_at = NOW() where quote_id = ?")
public class Quote {
  @Id
  @GeneratedValue
  @Column(name = "quote_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  private QuoteStatus status;

  private String sentence;
  private String author;

  @CreatedDate private LocalDateTime createdAt;
  @LastModifiedDate private LocalDateTime updatedAt;
  private boolean deleted = false;
  private LocalDateTime deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = true)
  private Member member;
}
