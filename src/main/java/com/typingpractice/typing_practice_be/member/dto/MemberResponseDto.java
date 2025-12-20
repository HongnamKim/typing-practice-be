package com.typingpractice.typing_practice_be.member.dto;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;

import java.time.LocalDateTime;

public record MemberResponseDto(
    Long id, String email, String nickname, MemberRole role, LocalDateTime createdAt) {
  public static MemberResponseDto from(Member member) {
    return new MemberResponseDto(
        member.getId(),
        member.getEmail(),
        member.getNickname(),
        member.getRole(),
        member.getCreatedAt());
  }
}
