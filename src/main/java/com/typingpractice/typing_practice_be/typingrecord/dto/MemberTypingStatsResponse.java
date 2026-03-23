package com.typingpractice.typing_practice_be.typingrecord.dto;

import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
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
}
