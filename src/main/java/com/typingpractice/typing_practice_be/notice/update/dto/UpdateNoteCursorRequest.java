package com.typingpractice.typing_practice_be.notice.update.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

public record UpdateNoteCursorRequest(
    LocalDateTime cursorReleasedAt, Long cursorId, @Min(1) @Max(100) Integer size) {

  @AssertTrue(message = "cursorReleasedAt 과 cursorId 는 모두 있거나 모두 없어야 합니다.")
  public boolean isCursorValid() {
    return (cursorReleasedAt == null) == (cursorId == null);
  }

  public UpdateNoteCursor toCursor() {
    if (cursorReleasedAt == null) return null;
    return new UpdateNoteCursor(cursorReleasedAt, cursorId);
  }

  public int sizeOrDefault() {
    return size != null ? size : 20;
  }
}
