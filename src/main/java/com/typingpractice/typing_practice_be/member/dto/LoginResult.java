package com.typingpractice.typing_practice_be.member.dto;

import com.typingpractice.typing_practice_be.member.domain.Member;
import lombok.Getter;

@Getter
public class LoginResult {
  private final Member member;
  private final boolean isNewMember;

  private LoginResult(Member member, boolean isNewMember) {
    this.member = member;
    this.isNewMember = isNewMember;
  }

  public static LoginResult create(Member member, boolean isNewMember) {
    return new LoginResult(member, isNewMember);
  }
}
