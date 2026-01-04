package com.typingpractice.typing_practice_be.member.query;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberCreateQuery {
  private final String email;

  private final String password;

  private final String nickname;

  private MemberCreateQuery(String email, String password, String nickname) {
    this.email = email;
    this.password = password;
    this.nickname = nickname;
  }

  public static MemberCreateQuery of(String email, String password, String nickname) {
    return new MemberCreateQuery(email, password, nickname);
  }
}
