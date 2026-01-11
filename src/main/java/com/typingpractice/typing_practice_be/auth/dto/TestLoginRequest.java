package com.typingpractice.typing_practice_be.auth.dto;

import lombok.Getter;

@Getter
public class TestLoginRequest {
  private String providerId;

  public static TestLoginRequest create(String providerId) {
    TestLoginRequest request = new TestLoginRequest();
    request.providerId = providerId;

    return request;
  }
}
