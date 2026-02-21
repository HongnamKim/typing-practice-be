package com.typingpractice.typing_practice_be.typingrecord.query;

import com.typingpractice.typing_practice_be.typingrecord.domain.Typo;
import com.typingpractice.typing_practice_be.typingrecord.dto.TypingRecordRequest;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class TypingRecordQuery {

  private final Long quoteId;
  private final int cpm;
  private final float accuracy;
  private final int charLength;
  private final int resetCount;
  private final List<Typo> typos;

  private TypingRecordQuery(
      Long quoteId, int cpm, float accuracy, int charLength, int resetCount, List<Typo> typos) {
    this.quoteId = quoteId;
    this.cpm = cpm;
    this.accuracy = accuracy;
    this.charLength = charLength;
    this.resetCount = resetCount;
    this.typos = typos;
  }

  public static TypingRecordQuery from(TypingRecordRequest request) {
    return new TypingRecordQuery(
        request.getQuoteId(),
        request.getCpm(),
        request.getAccuracy(),
        request.getCharLength(),
        request.getResetCount(),
        request.toTypos());
  }
}
