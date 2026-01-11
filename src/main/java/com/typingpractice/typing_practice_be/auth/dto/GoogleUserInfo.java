package com.typingpractice.typing_practice_be.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GoogleUserInfo {
  @JsonProperty("id")
  private String providerId;

  private String email;
  private String name;
  private String picture;

  public static GoogleUserInfo create(
      String providerId, String email, String name, String picture) {
    return new GoogleUserInfo(providerId, email, name, picture);
  }
}
