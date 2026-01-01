package com.typingpractice.typing_practice_be.quote.query;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.query.PaginationQuery;
import com.typingpractice.typing_practice_be.quote.domain.QuoteOrderBy;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.QuotePaginationRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class QuotePaginationQuery extends PaginationQuery {

  private final QuoteStatus status;
  private final QuoteType type;
  private final QuoteOrderBy orderBy;

  private QuotePaginationQuery(
      int page,
      int size,
      SortDirection sortDirection,
      QuoteStatus status,
      QuoteType type,
      QuoteOrderBy orderBy) {
    super(page, size, sortDirection);

    this.status = status;
    this.type = type;
    this.orderBy = orderBy;
  }

  public static QuotePaginationQuery from(QuotePaginationRequest request) {
    return new QuotePaginationQuery(
        request.getPage(),
        request.getSize(),
        request.getSortDirection(),
        request.getStatus(),
        request.getType(),
        request.getOrderBy());
  }
}
