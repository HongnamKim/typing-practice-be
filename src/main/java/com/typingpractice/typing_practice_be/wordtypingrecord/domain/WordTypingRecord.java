package com.typingpractice.typing_practice_be.wordtypingrecord.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typingpractice.typing_practice_be.word.domain.WordDifficultyTier;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "wordTypingRecord")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordTypingRecord {
  private static final float WPM_UPPER_LIMIT = 300f;
  private static final float ACCURACY_LOWER_LIMIT = 0.5f;

  @Id private String id;

  private Long memberId;
  private String anonymousId;

  private WordLanguage language;
  private WordDifficultyTier difficulty;
  private int wordCount;

  private float wpm;
  private float accuracy;
  private int correctWordCount;
  private int incorrectWordCount;
  private long elapsedTimeMs;

  private boolean outlier;
  private LocalDateTime completedAt;

  private List<Long> wordIds;
  private List<WordDetail> wordDetails;

  public static WordTypingRecord create(
      Long memberId,
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
    WordTypingRecord record = new WordTypingRecord();
    record.memberId = memberId;
    record.anonymousId = anonymousId;
    record.language = language;
    record.difficulty = difficulty;
    record.wordCount = wordCount;
    record.wpm = wpm;
    record.accuracy = accuracy;
    record.correctWordCount = correctWordCount;
    record.incorrectWordCount = incorrectWordCount;
    record.elapsedTimeMs = elapsedTimeMs;
    record.outlier = wpm > WPM_UPPER_LIMIT || accuracy < ACCURACY_LOWER_LIMIT;
    record.completedAt = LocalDateTime.now();
    record.wordIds = wordIds;
    record.wordDetails = wordDetails;
    return record;
  }

  @JsonIgnore
  public boolean isLoggedIn() {
    return memberId != null;
  }
}
