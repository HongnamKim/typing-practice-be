package com.typingpractice.typing_practice_be.word.dto;

import com.typingpractice.typing_practice_be.word.domain.WordDifficultyTier;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import lombok.Getter;
import org.hibernate.validator.constraints.Range;

@Getter
public class WordRequest {
  private final WordLanguage language;
  private final WordDifficultyTier difficulty;

  @Range(min = 10, max = 100)
  private final int count;

  public WordRequest(WordLanguage language, WordDifficultyTier difficulty, Integer count) {
    this.language = language;
    this.difficulty = difficulty != null ? difficulty : WordDifficultyTier.RANDOM;
    this.count = count != null ? count : 25;
  }
}
