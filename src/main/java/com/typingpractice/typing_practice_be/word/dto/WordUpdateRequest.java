package com.typingpractice.typing_practice_be.word.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WordUpdateRequest {
  private String word;

  public static WordUpdateRequest create(String word) {
    WordUpdateRequest request = new WordUpdateRequest();
    request.word = word;

    return request;
  }
}
