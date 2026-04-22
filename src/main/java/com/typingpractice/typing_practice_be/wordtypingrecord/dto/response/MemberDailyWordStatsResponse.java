package com.typingpractice.typing_practice_be.wordtypingrecord.dto.response;

import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberDailyWordStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.MemberDailyWordAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypingSnapshot;
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
public class MemberDailyWordStatsResponse {
  private int days;
  private List<DayEntry> content;

  public static MemberDailyWordStatsResponse create(int days, List<DayEntry> content) {
    MemberDailyWordStatsResponse response = new MemberDailyWordStatsResponse();
    response.days = days;
    response.content = content;
    return response;
  }

  public static MemberDailyWordStatsResponse of(
      int days,
      List<MemberDailyWordStats> pgList,
      MemberDailyWordAggregation yesterday,
      TodayWordTypingSnapshot today,
      LocalDate todayDate) {
    List<DayEntry> content = new ArrayList<>();

    for (MemberDailyWordStats stats : pgList) {
      content.add(DayEntry.from(stats));
    }

    if (yesterday != null) {
      content.add(DayEntry.from(yesterday));
    }

    if (today.getTotalAttempts() > 0) {
      content.add(DayEntry.fromToday(today, todayDate));
    }

    if (content.size() > days) {
      content = content.subList(content.size() - days, content.size());
    }

    return create(days, content);
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class DayEntry {
    private LocalDate date;
    private int attempts;
    private int wordsAttempted;
    private float avgWpm;
    private float avgAcc;
    private float bestWpm;
    private float practiceTimeMin;

    /** from mongo */
    public static DayEntry from(MemberDailyWordAggregation agg) {
      DayEntry entry = new DayEntry();
      entry.date = agg.getDateAsLocalDate();
      entry.attempts = agg.getAttempts();
      entry.wordsAttempted = agg.getWordsAttempted();
      entry.avgWpm = agg.getAvgWpm();
      entry.avgAcc = agg.getAvgAcc();
      entry.bestWpm = agg.getBestWpm();
      entry.practiceTimeMin = agg.getPracticeTimeMin();
      return entry;
    }

    /** from Postgres */
    public static DayEntry from(MemberDailyWordStats stats) {
      DayEntry entry = new DayEntry();
      entry.date = stats.getDate();
      entry.attempts = stats.getAttempts();
      entry.wordsAttempted = stats.getWordsAttempted();
      entry.avgWpm = stats.getAvgWpm();
      entry.avgAcc = stats.getAvgAcc();
      entry.bestWpm = stats.getBestWpm();
      entry.practiceTimeMin = stats.getPracticeTimeMin();
      return entry;
    }

    /** from redis */
    public static DayEntry fromToday(TodayWordTypingSnapshot today, LocalDate date) {
      DayEntry entry = new DayEntry();
      entry.date = date;
      entry.attempts = today.getTotalAttempts();
      entry.wordsAttempted = today.getTotalWordsAttempted();
      entry.avgWpm = today.getAvgWpm();
      entry.avgAcc = today.getAvgAcc();
      entry.bestWpm = today.getBestWpm();
      entry.practiceTimeMin = today.getTotalPracticeTimeMin();
      return entry;
    }
  }
}
