package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WordTypingAggregation {
  private Long wordId;
  private WordLanguage language;
  private int validAttemptsCount;
  private float avgTimeMs;
  private float correctRate;

  public static WordTypingAggregation create(
      Long wordId,
      WordLanguage language,
      int validAttemptsCount,
      float avgTimeMs,
      float correctRate) {
    WordTypingAggregation agg = new WordTypingAggregation();
    agg.wordId = wordId;
    agg.language = language;
    agg.validAttemptsCount = validAttemptsCount;
    agg.avgTimeMs = avgTimeMs;
    agg.correctRate = correctRate;

    return agg;
  }
}
