package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class MemberDailyWordAggregation {
  private Long memberId;
  private String date; // MongoDB에서 "%Y-%m-%d" 포맷으로 수신
  private WordLanguage language;
  private int attempts;
  private int wordsAttempted;
  private float avgWpm;
  private float avgAcc;
  private float bestWpm;
  private float practiceTimeMin;

  public LocalDate getDateAsLocalDate() {
    return LocalDate.parse(date);
  }

  public static MemberDailyWordAggregation create(
      Long memberId,
      String date,
      WordLanguage language,
      int attempts,
      int wordsAttempted,
      float avgWpm,
      float avgAcc,
      float bestWpm,
      float practiceTimeMin) {
    MemberDailyWordAggregation agg = new MemberDailyWordAggregation();
    agg.memberId = memberId;
    agg.date = date;
    agg.language = language;
    agg.attempts = attempts;
    agg.wordsAttempted = wordsAttempted;
    agg.avgWpm = avgWpm;
    agg.avgAcc = avgAcc;
    agg.bestWpm = bestWpm;
    agg.practiceTimeMin = practiceTimeMin;
    return agg;
  }
}
