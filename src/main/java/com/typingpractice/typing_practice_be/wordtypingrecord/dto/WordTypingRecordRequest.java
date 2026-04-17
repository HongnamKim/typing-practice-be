package com.typingpractice.typing_practice_be.wordtypingrecord.dto;

import com.typingpractice.typing_practice_be.word.domain.WordDifficultyTier;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class WordTypingRecordRequest {
  @NotNull private WordLanguage language;
  @NotNull private WordDifficultyTier difficulty;
  @NotNull @Positive private Integer wordCount;

  private String anonymousId;

  @NotNull @PositiveOrZero private Float wpm;

  @NotNull
  @DecimalMin("0.0")
  @DecimalMax("1.0")
  private Float accuracy;

  @NotNull @PositiveOrZero private Integer correctWordCount;
  @NotNull @PositiveOrZero private Integer incorrectWordCount;
  @NotNull @PositiveOrZero private Long elapsedTimeMs;

  private List<Long> wordIds;

  @Valid private List<WordDetailRequest> wordDetails;
}
