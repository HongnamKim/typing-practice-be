package com.typingpractice.typing_practice_be.notice.update.dto;

import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import com.typingpractice.typing_practice_be.notice.dto.LocalizedTextRequest;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateUpdateNoteRequest(
    String version,
    LocalDateTime releasedAt,
    @Valid List<LocalizedTextRequest> newFeatures,
    @Valid List<LocalizedTextRequest> improvements,
    Boolean published) {

  public List<LocalizedText> newFeaturesAsValue() {
    return newFeatures == null
        ? null
        : newFeatures.stream().map(LocalizedTextRequest::toValue).toList();
  }

  public List<LocalizedText> improvementsAsValue() {
    return improvements == null
        ? null
        : improvements.stream().map(LocalizedTextRequest::toValue).toList();
  }
}
