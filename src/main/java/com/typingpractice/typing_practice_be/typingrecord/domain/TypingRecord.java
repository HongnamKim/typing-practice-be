package com.typingpractice.typing_practice_be.typingrecord.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "typingRecord")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class TypingRecord {
  private static final int CPM_UPPER_LIMIT = 1500;
  private static final float ACCURACY_LOWER_LIMIT = 0.5f;

  @Id private String id;

  private Long memberId;
  private String anonymousId;

  private Long quoteId;
  private QuoteType quoteType;

  private QuoteLanguage language;
  private int cpm;
  private float accuracy;
  private int charLength;
  private int resetCount;
  private boolean outlier;
  private List<Typo> typos;
  private LocalDateTime completedAt;
  private TrackingInfo tracking;

  private ServingType servingType;

  private float estimatedDifficulty;
  private float estimatedUncertainty;

  private float quoteDifficulty;
  private float avgCpmSnapshot;
  private float avgAccSnapshot;

  public static TypingRecord create(
      Long memberId,
      String anonymousId,
      Long quoteId,
      QuoteLanguage language,
      QuoteType quoteType,
      int cpm,
      float accuracy,
      int charLength,
      int resetCount,
      List<Typo> typos,
      TrackingInfo tracking,
      ServingType servingType,
      float estimatedDifficulty,
      float estimatedUncertainty,
      float quoteDifficulty,
      float avgCpmSnapshot,
      float avgAccSnapshot) {
    TypingRecord record = new TypingRecord();

    record.memberId = memberId;
    record.anonymousId = anonymousId;
    record.quoteId = quoteId;
    record.language = language;
    record.quoteType = quoteType;
    record.cpm = cpm;
    record.accuracy = accuracy;
    record.charLength = charLength;
    record.resetCount = resetCount;
    record.outlier = cpm > CPM_UPPER_LIMIT || accuracy < ACCURACY_LOWER_LIMIT;
    record.typos = typos;
    record.completedAt = LocalDateTime.now();
    record.tracking = tracking;

    record.servingType = servingType;
    record.estimatedDifficulty = estimatedDifficulty;
    record.estimatedUncertainty = estimatedUncertainty;
    record.quoteDifficulty = quoteDifficulty;
    record.avgCpmSnapshot = avgCpmSnapshot;
    record.avgAccSnapshot = avgAccSnapshot;
    return record;
  }

  @JsonIgnore
  public boolean isLoggedIn() {
    return memberId != null;
  }
}
