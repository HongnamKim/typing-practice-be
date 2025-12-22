package com.typingpractice.typing_practice_be.member.dto;

import com.typingpractice.typing_practice_be.member.domain.Member;

public record LoginResponse(Long id, String email, String nickname, String token) {
  public static LoginResponse of(Member member, String token) {
    return new LoginResponse(member.getId(), member.getEmail(), member.getNickname(), token);
  }
}
