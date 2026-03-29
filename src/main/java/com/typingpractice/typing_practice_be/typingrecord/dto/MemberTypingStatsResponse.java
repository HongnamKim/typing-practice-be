package com.typingpractice.typing_practice_be.typingrecord.dto;

import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypingAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypingSnapshot;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberTypingStatsResponse {
  private int totalAttempts;
  private float avgCpm;
  private float avgAcc;
  private int bestCpm;
  private float totalPracticeTimeMin;
  private int totalResetCount;

  public static MemberTypingStatsResponse create(
      int totalAttempts,
      float avgCpm,
      float avgAcc,
      int bestCpm,
      float totalPracticeTimeMin,
      int totalResetCount) {
    MemberTypingStatsResponse response = new MemberTypingStatsResponse();
    response.totalAttempts = totalAttempts;
    response.avgCpm = avgCpm;
    response.avgAcc = avgAcc;
    response.bestCpm = bestCpm;
    response.totalPracticeTimeMin = totalPracticeTimeMin;
    response.totalResetCount = totalResetCount;
    return response;
  }

  public static MemberTypingStatsResponse empty() {
    return new MemberTypingStatsResponse();
  }

  public static MemberTypingStatsResponse from(MemberTypingStats stats) {
    return create(
        stats.getTotalAttempts(),
        stats.getAvgCpm(),
        stats.getAvgAcc(),
        stats.getBestCpm(),
        stats.getTotalPracticeTimeMin(),
        stats.getTotalResetCount());
  }

  public static MemberTypingStatsResponse from(TodayTypingSnapshot today) {
    return create(
        today.getTotalAttempts(),
        today.getAvgCpm(),
        today.getAvgAcc(),
        today.getBestCpm(),
        today.getTotalPracticeTimeMin(),
        today.getTotalResetCount());
  }

  public static MemberTypingStatsResponse merge(MemberTypingStats pg, TodayTypingSnapshot today) {
    int mergedAttempts = pg.getTotalAttempts() + today.getTotalAttempts();
    float mergedAvgCpm =
        (pg.getAvgCpm() * pg.getTotalAttempts() + today.getAvgCpm() * today.getTotalAttempts())
            / mergedAttempts;
    float mergedAvgAcc =
        (pg.getAvgAcc() * pg.getTotalAttempts() + today.getAvgAcc() * today.getTotalAttempts())
            / mergedAttempts;

    return create(
        mergedAttempts,
        mergedAvgCpm,
        mergedAvgAcc,
        Math.max(pg.getBestCpm(), today.getBestCpm()),
        pg.getTotalPracticeTimeMin() + today.getTotalPracticeTimeMin(),
        pg.getTotalResetCount() + today.getTotalResetCount());
  }

  public static MemberTypingStatsResponse of(
      MemberTypingStats pg, MemberTypingAggregation yesterday, TodayTypingSnapshot today) {

    int totalAttempts = 0;
    float sumCpm = 0, sumAcc = 0;
    int bestCpm = 0;
    float totalPracticeTimeMin = 0;
    int totalResetCount = 0;

    if (pg != null) {
      totalAttempts += pg.getTotalAttempts();
      sumCpm += pg.getAvgCpm() * pg.getTotalAttempts();
      sumAcc += pg.getAvgAcc() * pg.getTotalAttempts();
      bestCpm = pg.getBestCpm();
      totalPracticeTimeMin += pg.getTotalPracticeTimeMin();
      totalResetCount += pg.getTotalResetCount();
    }

    if (yesterday != null) {
      totalAttempts += yesterday.getTotalAttempts();
      sumCpm += yesterday.getAvgCpm() * yesterday.getTotalAttempts();
      sumAcc += yesterday.getAvgAcc() * yesterday.getTotalAttempts();
      bestCpm = Math.max(bestCpm, yesterday.getBestCpm());
      totalPracticeTimeMin += yesterday.getTotalPracticeTimeMin();
      totalResetCount += yesterday.getTotalResetCount();
    }

    if (today.getTotalAttempts() > 0) {
      totalAttempts += today.getTotalAttempts();
      sumCpm += today.getAvgCpm() * today.getTotalAttempts();
      sumAcc += today.getAvgAcc() * today.getTotalAttempts();
      bestCpm = Math.max(bestCpm, today.getBestCpm());
      totalPracticeTimeMin += today.getTotalPracticeTimeMin();
      totalResetCount += today.getTotalResetCount();
    }

    if (totalAttempts == 0) return empty();

    return create(
        totalAttempts,
        sumCpm / totalAttempts,
        sumAcc / totalAttempts,
        bestCpm,
        totalPracticeTimeMin,
        totalResetCount);
  }
}
