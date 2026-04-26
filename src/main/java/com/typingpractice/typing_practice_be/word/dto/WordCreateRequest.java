package com.typingpractice.typing_practice_be.word.dto;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WordCreateRequest {
  @Size(max = 20)
  private String word;

  private WordLanguage language;

  public static WordCreateRequest create(String word, WordLanguage language) {
    WordCreateRequest request = new WordCreateRequest();
    request.word = word;
    request.language = language;

    return request;
  }
}
