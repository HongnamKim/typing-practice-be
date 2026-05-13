package com.typingpractice.typing_practice_be.notice.announcement.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

public record AnnouncementCursorRequest(
    LocalDateTime cursorPostedAt, Long cursorId, @Min(1) @Max(100) Integer size) {
  @AssertTrue(message = "cursorPostedAt 과 cursorId 는 모두 있거나 모두 없어야 합니다.")
  public boolean isCursorValid() {
    return (cursorPostedAt == null) == (cursorId == null);
  }

  public AnnouncementCursor toCursor() {
    if (cursorPostedAt == null) return null;
    return new AnnouncementCursor(cursorPostedAt, cursorId);
  }

  public int sizeOrDefault() {
    return size != null ? size : 20;
  }
}
