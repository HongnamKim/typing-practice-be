package com.typingpractice.typing_practice_be.common.query;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class PaginationQuery {
  private final int page;
  private final int size;
  private final SortDirection sortDirection;

  protected PaginationQuery(int page, int size, SortDirection sortDirection) {
    this.page = page;
    this.size = size;
    this.sortDirection = sortDirection;
  }
}
