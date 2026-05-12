package com.typingpractice.typing_practice_be.notice.update.dto;

import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import com.typingpractice.typing_practice_be.notice.dto.LocalizedTextRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;

public record CreateUpdateNoteRequest(
    @NotBlank
        @Pattern(
            regexp = "^v\\d+\\.\\d+\\.\\d+$",
            message = "버전은 v{major}.{minor}.{fix} 형식이어야 합니다. (예: v1.8.1)")
        String version,
    @NotNull LocalDateTime releasedAt,
    @NotNull @Valid List<LocalizedTextRequest> newFeatures,
    @NotNull @Valid List<LocalizedTextRequest> improvements,
    @NotNull Boolean published) {

  public List<LocalizedText> newFeaturesAsValue() {
    return newFeatures.stream().map(LocalizedTextRequest::toValue).toList();
  }

  public List<LocalizedText> improvementsAsValue() {
    return improvements.stream().map(LocalizedTextRequest::toValue).toList();
  }
}
