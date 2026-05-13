package com.typingpractice.typing_practice_be.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
public class CursorPage<T, C> {
	private final List<T> content;
	private final C nextCursor;
	private final boolean hasNext;
}
