package com.typingpractice.typing_practice_be.typingrecord.query;

import com.typingpractice.typing_practice_be.typingrecord.domain.ServingType;
import com.typingpractice.typing_practice_be.typingrecord.domain.TrackingInfo;
import com.typingpractice.typing_practice_be.typingrecord.domain.Typo;
import com.typingpractice.typing_practice_be.typingrecord.dto.request.TypingRecordRequest;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class TypingRecordQuery {

  private final Long quoteId;
  private final String anonymousId;
  private final int cpm;
  private final float accuracy;
  private final int charLength;
  private final int resetCount;
  private final List<Typo> typos;
  private final TrackingInfo tracking;
  private final ServingType servingType;

  private final float estimatedDifficulty;
  private final float estimatedUncertainty;

  private TypingRecordQuery(
      Long quoteId,
      int cpm,
      float accuracy,
      int charLength,
      int resetCount,
      List<Typo> typos,
      String anonymousId,
      TrackingInfo tracking,
      ServingType servingType,
      float estimatedDifficulty,
      float estimatedUncertainty) {
    this.quoteId = quoteId;
    this.cpm = cpm;
    this.accuracy = accuracy;
    this.charLength = charLength;
    this.resetCount = resetCount;
    this.typos = typos;
    this.anonymousId = anonymousId;
    this.tracking = tracking;
    this.servingType = servingType;

    this.estimatedDifficulty = estimatedDifficulty;
    this.estimatedUncertainty = estimatedUncertainty;
  }

  public static TypingRecordQuery from(TypingRecordRequest request) {
    TrackingInfo tracking = null;
    if (request.getTracking() != null) {
      tracking =
          TrackingInfo.create(
              request.getTracking().getSessionId(),
              request.getTracking().getReferrer(),
              request.getTracking().getDeviceType());
    }
    ServingType servingType =
        request.getServingType() != null ? request.getServingType() : ServingType.RANDOM;

    return new TypingRecordQuery(
        request.getQuoteId(),
        request.getCpm(),
        request.getAccuracy(),
        request.getCharLength(),
        request.getResetCount(),
        request.toTypos(),
        request.getAnonymousId(),
        tracking,
        servingType,
        request.getEstimatedDifficulty(),
        request.getEstimatedUncertainty());
  }
}
