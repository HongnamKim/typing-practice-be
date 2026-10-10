package com.typingpractice.typing_practice_be.typingrecord.dto.response;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 키보드 히트맵용 글자별 오타 횟수 (전체 글자) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTypoStatsAllResponse {
  private Content content;

  public static MemberTypoStatsAllResponse of(
      QuoteLanguage language,
      List<MemberTypoStats> pgList,
      List<MemberTypoAggregation> yesterdayList,
      TodayTypoSnapshot today) {
    Map<String, Integer> counts = new HashMap<>();

    for (MemberTypoStats stats : pgList) {
      counts.merge(stats.getExpected(), stats.getTypoCount(), Integer::sum);
    }

    for (MemberTypoAggregation agg : yesterdayList) {
      counts.merge(agg.getExpected(), agg.getCount(), Integer::sum);
    }

    // 오늘 Redis 키는 "{언어}:{정답}"
    String prefix = language + ":";
    for (Map.Entry<String, Integer> entry : today.getTypoCountMap().entrySet()) {
      counts.merge(entry.getKey().substring(prefix.length()), entry.getValue(), Integer::sum);
    }

    MemberTypoStatsAllResponse response = new MemberTypoStatsAllResponse();
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
    private QuoteLanguage language;
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
