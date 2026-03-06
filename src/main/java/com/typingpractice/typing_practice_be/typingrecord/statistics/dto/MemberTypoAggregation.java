package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberTypoAggregation {
  private Long memberId;
  private QuoteLanguage language;
  private String expected;
  private String actual;
  private int count;
  private int initialCount;
  private int medialCount;
  private int finalCount;
  private int letterCount;
}
