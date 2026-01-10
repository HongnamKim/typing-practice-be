package com.typingpractice.typing_practice_be.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
public class GoogleUserInfo {
  private String id;
  private String email;
  private String name;
  private String picture;
}
