package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuoteTypingAggregation {
  private Long quoteId;
  private String language;
  private int attemptsCount;
  private double avgCpm;
  private double avgAcc;
  private double avgResetCount;
}
