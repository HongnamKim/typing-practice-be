package com.typingpractice.typing_practice_be.notice.announcement.dto;

import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import jakarta.validation.constraints.NotBlank;

public record LocalizedTextRequest(@NotBlank String ko, String en, String ja) {
	public LocalizedText toValue() {
		return new LocalizedText(ko, en, ja);
	}
}
