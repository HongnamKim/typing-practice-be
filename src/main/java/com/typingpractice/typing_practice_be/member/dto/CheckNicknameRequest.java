package com.typingpractice.typing_practice_be.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
public class CheckNicknameRequest {
  @NotNull
  @Length(min = 2, max = 10)
  private String nickname;

  public static CheckNicknameRequest create(String nickname) {
    CheckNicknameRequest request = new CheckNicknameRequest();
    request.nickname = nickname;

    return request;
  }
}
