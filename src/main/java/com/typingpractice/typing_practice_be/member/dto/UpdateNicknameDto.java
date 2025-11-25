package com.typingpractice.typing_practice_be.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Getter
public class UpdateNicknameDto {
  @NotNull
  @Length(min = 2, max = 10)
  private String nickname;
}
