package com.typingpractice.typing_practice_be.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Getter
public class CreateMemberDto {
  @NotNull private String email;

  @NotNull private String password;

  @NotNull
  @Length(min = 2, max = 10)
  private String nickname;

  public static CreateMemberDto create(String email, String password, String nickname) {
    CreateMemberDto createMemberDto = new CreateMemberDto();
    createMemberDto.email = email;
    createMemberDto.password = password;
    createMemberDto.nickname = nickname;

    return createMemberDto;
  }
}
