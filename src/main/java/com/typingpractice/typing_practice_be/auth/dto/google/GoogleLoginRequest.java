package com.typingpractice.typing_practice_be.auth.dto.google;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GoogleLoginRequest {
  private String code;
}
