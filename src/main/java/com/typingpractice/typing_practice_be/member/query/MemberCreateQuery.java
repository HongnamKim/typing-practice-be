package com.typingpractice.typing_practice_be.member.query;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberCreateQuery {
  private final String providerId;

  private final String email;

  private final String nickname;

  private MemberCreateQuery(String providerId, String email, String nickname) {
    this.providerId = providerId;
    this.email = email;
    this.nickname = nickname;
  }

  public static MemberCreateQuery of(String providerId, String email, String nickname) {
    return new MemberCreateQuery(providerId, email, nickname);
  }
}
