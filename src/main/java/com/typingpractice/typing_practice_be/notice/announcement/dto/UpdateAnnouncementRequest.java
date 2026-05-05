package com.typingpractice.typing_practice_be.notice.announcement.dto;

import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import jakarta.validation.Valid;

import java.time.LocalDateTime;

public record UpdateAnnouncementRequest(
				LocalDateTime postedAt,
				@Valid LocalizedTextRequest title,
				@Valid LocalizedTextRequest content,
				Boolean published,
				Boolean pinned) {
	public LocalizedText titleAsValue() {
		return title == null ? null : title.toValue();
	}

	public LocalizedText contentAsValue() {
		return content == null ? null : content.toValue();
	}
}
