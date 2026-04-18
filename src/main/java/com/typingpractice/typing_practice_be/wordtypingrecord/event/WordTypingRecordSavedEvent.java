package com.typingpractice.typing_practice_be.wordtypingrecord.event;

import com.typingpractice.typing_practice_be.word.domain.WordDifficultyTier;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordDetail;
import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypingRecord;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class WordTypingRecordSavedEvent {
  private final Long memberId;
  private final WordLanguage language;
  private final WordDifficultyTier difficulty;
  private final int wordCount;
  private final float wpm;
  private final float accuracy;
  private final int correctWordCount;
  private final int incorrectWordCount;
  private final long elapsedTimeMs;
  private final boolean outlier;

  private final List<WordDetail> wordDetails;

  private WordTypingRecordSavedEvent(
      Long memberId,
      WordLanguage language,
      WordDifficultyTier difficulty,
      int wordCount,
      float wpm,
      float accuracy,
      int correctWordCount,
      int incorrectWordCount,
      long elapsedTimeMs,
      boolean outlier,
      List<WordDetail> wordDetails) {
    this.memberId = memberId;
    this.language = language;
    this.difficulty = difficulty;
    this.wordCount = wordCount;
    this.wpm = wpm;
    this.accuracy = accuracy;
    this.correctWordCount = correctWordCount;
    this.incorrectWordCount = incorrectWordCount;
    this.elapsedTimeMs = elapsedTimeMs;
    this.outlier = outlier;
    this.wordDetails = wordDetails;
  }

  public static WordTypingRecordSavedEvent from(WordTypingRecord record) {
    return new WordTypingRecordSavedEvent(
        record.getMemberId(),
        record.getLanguage(),
        record.getDifficulty(),
        record.getWordCount(),
        record.getWpm(),
        record.getAccuracy(),
        record.getCorrectWordCount(),
        record.getIncorrectWordCount(),
        record.getElapsedTimeMs(),
        record.isOutlier(),
        record.getWordDetails());
  }
}
