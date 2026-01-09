package com.typingpractice.typing_practice_be.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MemberLoginRequest {
  @NotNull private String email;

  @NotNull private String password;

  public static MemberLoginRequest create(String email, String password) {
    MemberLoginRequest memberLoginRequest = new MemberLoginRequest();
    memberLoginRequest.email = email;
    memberLoginRequest.password = password;

    return memberLoginRequest;
  }
}
