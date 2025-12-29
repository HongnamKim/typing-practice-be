package com.typingpractice.typing_practice_be.common.dto;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class PaginationRequest {
  @Positive(message = "1 이상")
  private final int page;

  @Positive(message = "1 이상")
  @Max(value = 100, message = "최대 100")
  private final int size;

  private final SortDirection sortDirection;

  protected PaginationRequest(Integer page, Integer size, SortDirection sortDirection) {
    this.page = page != null ? page : 1;
    this.size = size != null ? size : 50;
    this.sortDirection = sortDirection != null ? sortDirection : SortDirection.DESC;
  }
}
