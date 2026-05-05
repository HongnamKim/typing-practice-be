package com.typingpractice.typing_practice_be.notice.announcement.dto;

import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateAnnouncementRequest(
				@NotNull LocalDateTime postedAt,
				@NotNull @Valid LocalizedTextRequest title,
				@NotNull @Valid LocalizedTextRequest content,
				@NotNull Boolean published,
				@NotNull Boolean pinned
) {
	public LocalizedText titleAsValue() {
		return title.toValue();
	}

	public LocalizedText contentAsValue() {
		return content.toValue();
	}
}
