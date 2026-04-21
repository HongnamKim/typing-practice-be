package com.typingpractice.typing_practice_be.wordtypingrecord.dto;

import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypingStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.MemberWordTypingAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypingSnapshot;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberWordTypingStatsResponse {
  private int totalAttempts;
  private int totalWordsAttempted;
  private float avgWpm;
  private float avgAcc;
  private float bestWpm;
  private float totalPracticeTimeMin;

  public static MemberWordTypingStatsResponse create(
      int totalAttempts,
      int totalWordsAttempted,
      float avgWpm,
      float avgAcc,
      float bestWpm,
      float totalPracticeTimeMin) {
    MemberWordTypingStatsResponse response = new MemberWordTypingStatsResponse();
    response.totalAttempts = totalAttempts;
    response.totalWordsAttempted = totalWordsAttempted;
    response.avgWpm = avgWpm;
    response.avgAcc = avgAcc;
    response.bestWpm = bestWpm;
    response.totalPracticeTimeMin = totalPracticeTimeMin;
    return response;
  }

  public static MemberWordTypingStatsResponse empty() {
    return new MemberWordTypingStatsResponse();
  }

  public static MemberWordTypingStatsResponse of(
      MemberWordTypingStats pg,
      MemberWordTypingAggregation yesterday,
      TodayWordTypingSnapshot today) {
    int totalAttempts = 0;
    int totalWordsAttempted = 0;
    float sumWpm = 0f;
    float sumAcc = 0f;
    float bestWpm = 0f;
    float totalPracticeTimeMin = 0f;

    if (pg != null) {
      totalAttempts += pg.getTotalAttempts();
      totalWordsAttempted += pg.getTotalWordsAttempted();
      sumWpm += pg.getAvgWpm() * pg.getTotalAttempts();
      sumAcc += pg.getAvgAcc() * pg.getTotalAttempts();
      bestWpm = pg.getBestWpm();
      totalPracticeTimeMin += pg.getTotalPracticeTimeMin();
    }

    if (yesterday != null) {
      totalAttempts += yesterday.getTotalAttempts();
      totalWordsAttempted += yesterday.getTotalWordsAttempted();
      sumWpm += yesterday.getAvgWpm() * yesterday.getTotalAttempts();
      sumAcc += yesterday.getAvgAcc() * yesterday.getTotalAttempts();
      bestWpm = Math.max(bestWpm, yesterday.getBestWpm());
      totalPracticeTimeMin += yesterday.getTotalPracticeTimeMin();
    }

    if (today.getTotalAttempts() > 0) {
      totalAttempts += today.getTotalAttempts();
      totalWordsAttempted += today.getTotalWordsAttempted();
      sumWpm += today.getAvgWpm() * today.getTotalAttempts();
      sumAcc += today.getAvgAcc() * today.getTotalAttempts();
      bestWpm = Math.max(bestWpm, today.getBestWpm());
      totalPracticeTimeMin += today.getTotalPracticeTimeMin();
    }

    if (totalAttempts == 0) return empty();

    return create(
        totalAttempts,
        totalWordsAttempted,
        sumWpm / totalAttempts,
        sumAcc / totalAttempts,
        bestWpm,
        totalPracticeTimeMin);
  }
}
