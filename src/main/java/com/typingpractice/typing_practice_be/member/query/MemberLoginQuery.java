package com.typingpractice.typing_practice_be.member.query;

import com.typingpractice.typing_practice_be.member.dto.LoginRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberLoginQuery {
  private final String email;
  private final String password;

  private MemberLoginQuery(String email, String password) {
    this.email = email;
    this.password = password;
  }

  public static MemberLoginQuery from(LoginRequest request) {
    return new MemberLoginQuery(request.getEmail(), request.getPassword());
  }
}
