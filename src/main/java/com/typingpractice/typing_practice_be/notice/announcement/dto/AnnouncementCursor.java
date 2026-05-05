package com.typingpractice.typing_practice_be.notice.announcement.dto;

import java.time.LocalDateTime;

public record AnnouncementCursor(LocalDateTime postedAt, Long id) {
}
