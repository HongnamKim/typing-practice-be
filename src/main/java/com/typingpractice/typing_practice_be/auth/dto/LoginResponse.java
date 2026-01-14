package com.typingpractice.typing_practice_be.auth.dto;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.dto.LoginResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponse {
  private Long id;
  private String email;
  private String nickname;
  private MemberRole role;
  private LocalDateTime createdAt;

  private boolean isNewMember;

  private String accessToken;
  private String refreshToken;

  public static LoginResponse from(LoginResult result, String accessToken, String refreshToken) {
    Member member = result.getMember();
    return new LoginResponse(
        member.getId(),
        member.getEmail(),
        member.getNickname(),
        member.getRole(),
        member.getCreatedAt(),
        result.isNewMember(),
        accessToken,
        refreshToken);
  }
}
