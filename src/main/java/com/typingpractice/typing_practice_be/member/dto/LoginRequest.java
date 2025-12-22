package com.typingpractice.typing_practice_be.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LoginRequest {
  @NotNull private String email;

  @NotNull private String password;

  public static LoginRequest create(String email, String password) {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.email = email;
    loginRequest.password = password;

    return loginRequest;
  }
}
