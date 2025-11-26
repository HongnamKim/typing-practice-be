package com.typingpractice.typing_practice_be.member.dto;

import com.typingpractice.typing_practice_be.member.domain.Member;

public record LoginResponseDto(Long id, String email, String nickname, String token) {
  public static LoginResponseDto of(Member member, String token) {
    return new LoginResponseDto(member.getId(), member.getEmail(), member.getNickname(), token);
  }
}
