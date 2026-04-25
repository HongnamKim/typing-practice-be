package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberWordTypoAggregation {
  private Long memberId;
  private WordLanguage language;
  private String expected;
  private String actual;
  private int count;
  private int initialCount;
  private int medialCount;
  private int finalCount;
  private int letterCount;
}
