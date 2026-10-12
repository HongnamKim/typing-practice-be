package com.typingpractice.typing_practice_be.member.dto.admin;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import java.time.LocalDateTime;

public record AdminMemberResponse(
    Long id,
    String email,
    String nickname,
    MemberRole role,
    LocalDateTime createdAt,
    LocalDateTime lastPracticedAt) {
  public static AdminMemberResponse from(MemberWithLastPracticedAt row) {
    Member member = row.member();
    return new AdminMemberResponse(
        member.getId(),
        member.getEmail(),
        member.getNickname(),
        member.getRole(),
        member.getCreatedAt(),
        row.lastPracticedAt());
  }
}
