package com.typingpractice.typing_practice_be.member.dto.admin;

import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberUpdateRoleRequest {
  @NotNull(message = "필수값 누락")
  private MemberRole role;
}
