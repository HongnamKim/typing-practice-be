package com.typingpractice.typing_practice_be.typingrecord.dto;

import com.typingpractice.typing_practice_be.typingrecord.domain.Typo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class TypingRecordRequest {
  @NotNull @Positive private Long quoteId;

  private String anonymousId; // 비회원 식별자

  @NotNull @Positive private Integer cpm;

  @NotNull private Float accuracy;

  @NotNull @Positive private Integer charLength;

  @NotNull private Integer resetCount;

  @Valid private List<TypoRequest> typos;

  private TrackingRequest tracking;

  public static TypingRecordRequest create(
      Long quoteId,
      String anonymousId,
      Integer cpm,
      Float accuracy,
      Integer charLength,
      Integer resetCount,
      List<TypoRequest> typos,
      TrackingRequest tracking) {
    TypingRecordRequest request = new TypingRecordRequest();

    request.quoteId = quoteId;
    request.anonymousId = anonymousId;
    request.cpm = cpm;
    request.accuracy = accuracy;
    request.charLength = charLength;
    request.resetCount = resetCount;
    request.typos = typos;
    request.tracking = tracking;

    return request;
  }

  public List<Typo> toTypos() {
    if (typos == null || typos.isEmpty()) {
      return List.of();
    }
    return typos.stream().map(TypoRequest::toTypo).toList();
  }
}
