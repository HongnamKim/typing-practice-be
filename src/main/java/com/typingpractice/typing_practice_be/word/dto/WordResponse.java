package com.typingpractice.typing_practice_be.word.dto;

import com.typingpractice.typing_practice_be.word.domain.Word;
import lombok.Getter;

@Getter
public class WordResponse {
  private Long wordId;
  private String word;
  private Float difficulty;

  public static WordResponse from(Word word) {
    WordResponse response = new WordResponse();
    response.wordId = word.getId();
    response.word = word.getWord();
    response.difficulty = word.getDifficulty();

    return response;
  }
}
