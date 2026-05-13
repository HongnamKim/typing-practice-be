package com.typingpractice.typing_practice_be.notice.update.dto;

import java.time.LocalDateTime;

public record UpdateNoteCursor(LocalDateTime releasedAt, Long id) {}
