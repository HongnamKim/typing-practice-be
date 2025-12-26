package com.typingpractice.typing_practice_be.member.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.report.domain.Report;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE member SET deleted = true, deleted_at = NOW() where member_id = ?")
public class Member extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "member_id")
  private Long id;

  private String email;
  private String password;

  private String nickname;

  @Enumerated(EnumType.STRING)
  private MemberRole role;

  private String banReason = "";

  private int reportCount = 0;
  private LocalDate lastReportDate = LocalDate.now();

  @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
  private List<Report> reports = new ArrayList<>();

  public static Member createMember(String email, String password, String nickname) {
    Member member = new Member();
    member.email = email;
    member.password = password;
    member.nickname = nickname;
    member.role = MemberRole.USER;
    member.reportCount = 0;
    member.lastReportDate = LocalDate.now();

    return member;
  }

  public void updateNickName(String nickname) {
    this.nickname = nickname;
  }

  public void updateRole(MemberRole role) {
    this.role = role;
  }

  public boolean canReportToday(int limit) {
    // 날짜 변경됨.
    if (LocalDate.now().isAfter(lastReportDate)) {
      return true;
    }

    return this.reportCount < limit;
  }

  public void incrementReportCount() {
    LocalDate today = LocalDate.now();
    if (today.isAfter(this.lastReportDate)) {
      this.reportCount = 0;
      this.lastReportDate = today;
    }

    this.reportCount++;
  }

  public void ban(String reason) {
    this.role = MemberRole.BANNED;

    if (reason != null) {
      this.banReason = reason;
    }
  }

  public void unban() {
    this.role = MemberRole.USER;
    this.banReason = "";
  }
}
