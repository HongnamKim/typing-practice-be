package com.typingpractice.typing_practice_be.member.dto.admin;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberBanRequest {
  private String banReason;
}
