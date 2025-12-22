package com.typingpractice.typing_practice_be.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PaginationResponse {
  private int page;
  private int size;
  private boolean hasNext;
}
