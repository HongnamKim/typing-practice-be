package com.typingpractice.typing_practice_be.wordtypingrecord.query;

import com.typingpractice.typing_practice_be.word.domain.WordDifficultyTier;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordDetail;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.WordDetailRequest;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.WordTypingRecordRequest;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class WordTypingRecordQuery {
  private final String anonymousId;
  private final WordLanguage language;
  private final WordDifficultyTier difficulty;
  private final int wordCount;
  private final float wpm;
  private final float accuracy;
  private final int correctWordCount;
  private final int incorrectWordCount;
  private final long elapsedTimeMs;
  private final List<Long> wordIds;
  private final List<WordDetail> wordDetails;

  private WordTypingRecordQuery(
      String anonymousId,
      WordLanguage language,
      WordDifficultyTier difficulty,
      int wordCount,
      float wpm,
      float accuracy,
      int correctWordCount,
      int incorrectWordCount,
      long elapsedTimeMs,
      List<Long> wordIds,
      List<WordDetail> wordDetails) {
    this.anonymousId = anonymousId;
    this.language = language;
    this.difficulty = difficulty;
    this.wordCount = wordCount;
    this.wpm = wpm;
    this.accuracy = accuracy;
    this.correctWordCount = correctWordCount;
    this.incorrectWordCount = incorrectWordCount;
    this.elapsedTimeMs = elapsedTimeMs;
    this.wordIds = wordIds;
    this.wordDetails = wordDetails;
  }

  public static WordTypingRecordQuery from(WordTypingRecordRequest request) {
    List<WordDetail> wordDetails =
        (request.getWordDetails() == null || request.getWordDetails().isEmpty())
            ? List.of()
            : request.getWordDetails().stream().map(WordDetailRequest::toWordDetail).toList();

    List<Long> wordIds = request.getWordIds() != null ? request.getWordIds() : List.of();

    return new WordTypingRecordQuery(
        request.getAnonymousId(),
        request.getLanguage(),
        request.getDifficulty(),
        request.getWordCount(),
        request.getWpm(),
        request.getAccuracy(),
        request.getCorrectWordCount(),
        request.getIncorrectWordCount(),
        request.getElapsedTimeMs(),
        wordIds,
        wordDetails);
  }
}
