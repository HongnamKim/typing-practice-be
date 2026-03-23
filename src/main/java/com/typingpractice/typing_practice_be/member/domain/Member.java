package com.typingpractice.typing_practice_be.member.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.report.domain.Report;
import jakarta.persistence.*;
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

  @Column(unique = true)
  private String providerId;

  private String email;

  @Column(unique = true)
  private String nickname;

  @Enumerated(EnumType.STRING)
  private MemberRole role;

  private String banReason = "";

  @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
  private List<Report> reports = new ArrayList<>();

  public static String DEFAULT_NICKNAME = "nickname";

  public static Member createMember(String providerId, String email, String nickname) {
    Member member = new Member();
    member.providerId = providerId;
    member.email = email;
    member.nickname = nickname != null ? nickname : DEFAULT_NICKNAME;
    member.role = MemberRole.USER;

    return member;
  }

  public void updateNickName(String nickname) {
    this.nickname = nickname;
  }

  public void updateRole(MemberRole role) {
    this.role = role;
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
