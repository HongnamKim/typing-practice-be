package com.typingpractice.typing_practice_be.member.dto;

import com.typingpractice.typing_practice_be.member.domain.Member;

import java.time.LocalDateTime;

public record MemberResponseDto(Long id, String email, String nickname, LocalDateTime createdAt) {
  public static MemberResponseDto from(Member member) {
    return new MemberResponseDto(
        member.getId(), member.getEmail(), member.getNickname(), member.getCreatedAt());
  }
}
