package com.typingpractice.typing_practice_be.auth.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GoogleLoginRequest {
  private String code;
}
