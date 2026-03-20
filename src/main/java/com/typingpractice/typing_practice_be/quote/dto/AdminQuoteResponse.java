package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminQuoteResponse extends QuoteResponse {
  private TypingStatsInfo typingStats;

  public static AdminQuoteResponse from(Quote quote) {
    AdminQuoteResponse response = new AdminQuoteResponse();
    response.fillFrom(quote);

    if (quote.getTypingStats() != null) {
      response.typingStats = TypingStatsInfo.from(quote.getTypingStats());
    }

    return response;
  }

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class TypingStatsInfo {
    private int totalAttemptsCount;
    private int validAttemptsCount;
    private float avgCpm;
    private float avgAcc;
    private float avgResetCount;

    public static TypingStatsInfo from(QuoteTypingStats stats) {
      TypingStatsInfo info = new TypingStatsInfo();
      info.totalAttemptsCount = stats.getTotalAttemptsCount();
      info.validAttemptsCount = stats.getValidAttemptsCount();
      info.avgCpm = stats.getAvgCpm();
      info.avgAcc = stats.getAvgAcc();
      info.avgResetCount = stats.getAvgResetCount();
      return info;
    }
  }
}
