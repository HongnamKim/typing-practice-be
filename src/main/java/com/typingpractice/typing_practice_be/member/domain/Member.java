package com.typingpractice.typing_practice_be.member.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE member SET deleted = true, deleted_at = NOW() where member_id = ?")
public class Member {
  @Id
  @GeneratedValue
  @Column(name = "member_id")
  private Long id;

  private String email;
  private String password;

  private String nickname;

  @Enumerated(EnumType.STRING)
  private MemberRole role;

  @CreatedDate private LocalDateTime createdAt;
  @LastModifiedDate private LocalDateTime updatedAt;
  private boolean deleted = false;
  private LocalDateTime deletedAt;

  public static Member createMember(String email, String password, String nickname) {
    Member member = new Member();
    member.email = email;
    member.password = password;
    member.nickname = nickname;
    member.role = MemberRole.USER;

    return member;
  }

  public void updateNickName(String nickname) {
    this.nickname = nickname;
  }
}
