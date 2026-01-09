package com.typingpractice.typing_practice_be.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
public class PageResult<T> {
  private final List<T> content;
  private final int page;
  private final int size;
  private final boolean hasNext;
}
