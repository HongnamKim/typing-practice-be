package com.typingpractice.typing_practice_be.wordtypingrecord.dto.response;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 키보드 히트맵용 글자별 오타 횟수 (전체 글자) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWordTypoStatsAllResponse {
  private Content content;

  public static MemberWordTypoStatsAllResponse of(
      WordLanguage language,
      List<MemberWordTypoStats> pgList,
      List<MemberWordTypoAggregation> yesterdayList,
      TodayWordTypoSnapshot today) {
    Map<String, Integer> counts = new HashMap<>();

    for (MemberWordTypoStats stats : pgList) {
      counts.merge(stats.getExpected(), stats.getTypoCount(), Integer::sum);
    }

    for (MemberWordTypoAggregation agg : yesterdayList) {
      counts.merge(agg.getExpected(), agg.getCount(), Integer::sum);
    }

    // 오늘 Redis 키는 "{언어}:{정답}"
    String prefix = language + ":";
    for (Map.Entry<String, Integer> entry : today.getTypoCountMap().entrySet()) {
      counts.merge(entry.getKey().substring(prefix.length()), entry.getValue(), Integer::sum);
    }

    MemberWordTypoStatsAllResponse response = new MemberWordTypoStatsAllResponse();
    response.content = new Content();
    response.content.language = language;
    response.content.typos =
        counts.entrySet().stream()
            .map(e -> TypoEntry.of(e.getKey(), e.getValue()))
            .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
            .toList();
    return response;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class Content {
    private WordLanguage language;
    private List<TypoEntry> typos;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class TypoEntry {
    private String expected;
    private int count;

    private static TypoEntry of(String expected, int count) {
      TypoEntry entry = new TypoEntry();
      entry.expected = expected;
      entry.count = count;
      return entry;
    }
  }
}
