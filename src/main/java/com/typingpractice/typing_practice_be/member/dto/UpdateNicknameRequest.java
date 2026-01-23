package com.typingpractice.typing_practice_be.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Getter
public class UpdateNicknameRequest {
  @NotNull
  @NotBlank
  @Length(min = 2, max = 10)
  private String nickname;

  public static UpdateNicknameRequest create(String nickname) {
    UpdateNicknameRequest request = new UpdateNicknameRequest();

    request.nickname = nickname;

    return request;
  }
}
