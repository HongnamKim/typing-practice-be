package com.typingpractice.typing_practice_be.common.dto;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public abstract class PaginationRequest {
  @Positive(message = "1 이상")
  private int page = 1;

  @Positive(message = "1 이상")
  @Max(value = 100, message = "최대 100")
  private int size = 10;

  private SortDirection sortDirection = SortDirection.ASC;

  protected PaginationRequest(int page, int size, SortDirection sortDirection) {
    this.page = page;
    this.size = size;
    this.sortDirection = sortDirection;
  }
}
