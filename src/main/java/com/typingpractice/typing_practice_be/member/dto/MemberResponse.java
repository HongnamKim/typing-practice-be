package com.typingpractice.typing_practice_be.member.dto;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;

import java.time.LocalDateTime;

public record MemberResponse(
    Long id, String email, String nickname, MemberRole role, LocalDateTime createdAt) {
  public static MemberResponse from(Member member) {
    return new MemberResponse(
        member.getId(),
        member.getEmail(),
        member.getNickname(),
        member.getRole(),
        member.getCreatedAt());
  }
}
