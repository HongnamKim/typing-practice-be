package com.typingpractice.typing_practice_be.typingrecord.domain;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "typingRecord")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TypingRecord {
  private static final int CPM_UPPER_LIMIT = 1500;
  private static final float ACCURACY_LOWER_LIMIT = 0.5f;

  @Id private String id;

  private Long memberId;
  private Long quoteId;
  private QuoteLanguage language;
  private int cpm;
  private float accuracy;
  private int charLength;
  private int resetCount;
  private boolean outlier;
  private List<Typo> typos;
  private LocalDateTime completedAt;

  public static TypingRecord create(
      Long memberId,
      Long quoteId,
      QuoteLanguage language,
      int cpm,
      float accuracy,
      int charLength,
      int resetCount,
      List<Typo> typos) {
    TypingRecord record = new TypingRecord();

    record.memberId = memberId;
    record.quoteId = quoteId;
    record.language = language;
    record.cpm = cpm;
    record.accuracy = accuracy;
    record.charLength = charLength;
    record.resetCount = resetCount;
    record.outlier = cpm > CPM_UPPER_LIMIT || accuracy < ACCURACY_LOWER_LIMIT;
    record.typos = typos;
    record.completedAt = LocalDateTime.now();
    return record;
  }

  public boolean isLoggedIn() {
    return memberId != null;
  }
}
