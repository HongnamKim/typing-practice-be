package com.typingpractice.typing_practice_be.typingrecord.event;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.domain.TypingRecord;
import com.typingpractice.typing_practice_be.typingrecord.domain.Typo;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class TypingRecordSavedEvent {
  private final Long memberId;
  private final QuoteLanguage language;
  private final int cpm;
  private final float accuracy;
  private final int charLength;
  private final int resetCount;
  private final boolean outlier;
  private final List<Typo> typos;

  private TypingRecordSavedEvent(
      Long memberId,
      QuoteLanguage language,
      int cpm,
      float accuracy,
      int charLength,
      int resetCount,
      boolean outlier,
      List<Typo> typos) {
    this.memberId = memberId;
    this.language = language;
    this.cpm = cpm;
    this.accuracy = accuracy;
    this.charLength = charLength;
    this.resetCount = resetCount;
    this.outlier = outlier;
    this.typos = typos;
  }

  public static TypingRecordSavedEvent from(TypingRecord record) {
    return new TypingRecordSavedEvent(
        record.getMemberId(),
        record.getLanguage(),
        record.getCpm(),
        record.getAccuracy(),
        record.getCharLength(),
        record.getResetCount(),
        record.isOutlier(),
        record.getTypos());
  }
}
