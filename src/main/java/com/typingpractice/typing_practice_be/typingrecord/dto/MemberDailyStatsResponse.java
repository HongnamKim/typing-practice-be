package com.typingpractice.typing_practice_be.typingrecord.dto;

import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberDailyStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypingSnapshot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberDailyStatsResponse {
  private int days;
  private List<DayEntry> content;

  public static MemberDailyStatsResponse create(int days, List<DayEntry> content) {
    MemberDailyStatsResponse response = new MemberDailyStatsResponse();
    response.days = days;
    response.content = content;
    return response;
  }

  public static MemberDailyStatsResponse of(
      int days, List<MemberDailyStats> pgList, TodayTypingSnapshot today, LocalDate todayDate) {
    List<DayEntry> content = new ArrayList<>();

    for (MemberDailyStats stats : pgList) {
      content.add(DayEntry.from(stats));
    }

    if (today.getTotalAttempts() > 0) {
      content.add(DayEntry.fromToday(today, todayDate));
    }

    return create(days, content);
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class DayEntry {
    private LocalDate date;
    private int attempts;
    private float avgCpm;
    private float avgAcc;
    private int bestCpm;
    private int resetCount;
    private float practiceTimeMin;

    public static DayEntry from(MemberDailyStats stats) {
      DayEntry entry = new DayEntry();
      entry.date = stats.getDate();
      entry.attempts = stats.getAttempts();
      entry.avgCpm = stats.getAvgCpm();
      entry.avgAcc = stats.getAvgAcc();
      entry.bestCpm = stats.getBestCpm();
      entry.resetCount = stats.getResetCount();
      entry.practiceTimeMin = stats.getPracticeTimeMin();
      return entry;
    }

    public static DayEntry fromToday(TodayTypingSnapshot today, LocalDate date) {
      DayEntry entry = new DayEntry();
      entry.date = date;
      entry.attempts = today.getTotalAttempts();
      entry.avgCpm = today.getAvgCpm();
      entry.avgAcc = today.getAvgAcc();
      entry.bestCpm = today.getBestCpm();
      entry.resetCount = today.getTotalResetCount();
      entry.practiceTimeMin = today.getTotalPracticeTimeMin();
      return entry;
    }
  }
}
