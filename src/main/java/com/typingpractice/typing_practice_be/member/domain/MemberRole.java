package com.typingpractice.typing_practice_be.member.domain;

import lombok.Getter;

@Getter
public enum MemberRole {
  USER("ROLE_USER"),
  ADMIN("ROLE_ADMIN"),
  BANNED("ROLE_BANNED");

  private final String authority;

  MemberRole(String authority) {
    this.authority = authority;
  }
}
