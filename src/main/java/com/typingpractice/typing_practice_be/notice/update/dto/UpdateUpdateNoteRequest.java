package com.typingpractice.typing_practice_be.notice.update.dto;

import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import com.typingpractice.typing_practice_be.notice.dto.LocalizedTextRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateUpdateNoteRequest(
    @Pattern(
            regexp = "^v\\d+\\.\\d+\\.\\d+$",
            message = "버전은 v{major}.{minor}.{fix} 형식이어야 합니다. (예: v1.8.1)")
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
