package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.dto.PaginationRequest;
import com.typingpractice.typing_practice_be.quote.domain.QuoteOrderBy;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class QuotePaginationRequest extends PaginationRequest {
  private final QuoteStatus status;
  private final QuoteType type;
  private final QuoteOrderBy orderBy;

  public QuotePaginationRequest(
      Integer page,
      Integer size,
      SortDirection sortDirection,
      QuoteStatus status,
      QuoteType type,
      QuoteOrderBy orderBy) {
    super(page, size, sortDirection);
    this.status = status;
    this.type = type;
    this.orderBy = orderBy != null ? orderBy : QuoteOrderBy.id;
  }

  /*protected QuotePaginationRequest(int page, int size, SortDirection sortDirection) {
    super(page, size, sortDirection);
  }

  public static QuotePaginationRequest create(
      int page,
      int size,
      SortDirection sortDirection,
      QuoteStatus status,
      QuoteType type,
      QuoteOrderBy orderBy) {
    QuotePaginationRequest request = new QuotePaginationRequest(page, size, sortDirection);

    request.status = status;
    request.type = type;
    request.orderBy = orderBy;

    return request;
  }*/
}
