package com.typingpractice.typing_practice_be.quote.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuoteIdWithDifficulty {
  private Long id;
  private float difficulty;

  public static QuoteIdWithDifficulty create(Long id, float difficulty) {
    QuoteIdWithDifficulty dto = new QuoteIdWithDifficulty();
    dto.id = id;
    dto.difficulty = difficulty;

    return dto;
  }
}
