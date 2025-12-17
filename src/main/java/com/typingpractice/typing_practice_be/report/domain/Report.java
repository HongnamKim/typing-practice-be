package com.typingpractice.typing_practice_be.report.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE report SET deleted = true, deleted_at = NOW() where report_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "report_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  private ReportStatus status; // 처리 상태

  @Enumerated(EnumType.STRING)
  private ReportReason reason; // 사유: 수정, 삭제

  private String detail; // 상세 사유

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "quote_id")
  private Quote quote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  public static Report create(Member member, Quote quote, ReportReason reason, String detail) {
    Report report = new Report();
    report.status = ReportStatus.PENDING;
    report.reason = reason;
    report.detail = detail;

    report.quote = quote;
    report.member = member;

    return report;
  }

  public void updateStatus(ReportStatus status) {
    this.status = status;
  }
}
