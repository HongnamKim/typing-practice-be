package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class MemberWordTypingAggregation {
  private Long memberId;
  private WordLanguage language;
  private int totalAttempts;
  private int totalWordsAttempted;
  private float avgWpm;
  private float avgAcc;
  private float bestWpm;
  private float totalPracticeTimeMin;
  private LocalDateTime lastPracticedAt;

  public static MemberWordTypingAggregation create(
      Long memberId,
      WordLanguage language,
      int totalAttempts,
      int totalWordsAttempted,
      float avgWpm,
      float avgAcc,
      float bestWpm,
      float totalPracticeTimeMin,
      LocalDateTime lastPracticedAt) {
    MemberWordTypingAggregation agg = new MemberWordTypingAggregation();
    agg.memberId = memberId;
    agg.language = language;
    agg.totalAttempts = totalAttempts;
    agg.totalWordsAttempted = totalWordsAttempted;
    agg.avgWpm = avgWpm;
    agg.avgAcc = avgAcc;
    agg.bestWpm = bestWpm;
    agg.totalPracticeTimeMin = totalPracticeTimeMin;
    agg.lastPracticedAt = lastPracticedAt;

    return agg;
  }
}
