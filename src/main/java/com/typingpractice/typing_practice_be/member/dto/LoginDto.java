package com.typingpractice.typing_practice_be.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LoginDto {
  @NotNull private String email;

  @NotNull private String password;

  public static LoginDto create(String email, String password) {
    LoginDto loginDto = new LoginDto();
    loginDto.email = email;
    loginDto.password = password;

    return loginDto;
  }
}
