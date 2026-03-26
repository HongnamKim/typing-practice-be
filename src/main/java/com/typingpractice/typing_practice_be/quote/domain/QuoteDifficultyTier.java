package com.typingpractice.typing_practice_be.quote.domain;

import lombok.Getter;

@Getter
public enum QuoteDifficultyTier {
	ALL(null, null),
	EASY(0f, 30f),
	NORMAL(31f, 60f),
	HARD(61f, 100f);

	private final Float min;
	private final Float max;

	QuoteDifficultyTier(Float min, Float max) {
		this.min = min;
		this.max = max;
	}
}
