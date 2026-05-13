package com.typingpractice.typing_practice_be.notice.update.dto;

import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import com.typingpractice.typing_practice_be.notice.update.domain.UpdateNote;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateNoteResponse(
    Long id,
    String version,
    LocalDateTime releasedAt,
    List<LocalizedText> newFeatures,
    List<LocalizedText> improvements,
    boolean published,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static UpdateNoteResponse from(UpdateNote note) {
    return new UpdateNoteResponse(
        note.getId(),
        note.getVersion(),
        note.getReleasedAt(),
        note.getNewFeatures(),
        note.getImprovements(),
        note.isPublished(),
        note.getCreatedAt(),
        note.getUpdatedAt());
  }
}
