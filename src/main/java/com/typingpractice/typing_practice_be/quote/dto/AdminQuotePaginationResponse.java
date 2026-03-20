package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.common.dto.PaginationResponse;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminQuotePaginationResponse extends PaginationResponse {
  private List<AdminQuoteResponse> content;

  protected AdminQuotePaginationResponse(int page, int size, boolean hasNext) {
    super(page, size, hasNext);
  }

  public static AdminQuotePaginationResponse from(PageResult<Quote> result) {
    AdminQuotePaginationResponse response =
        new AdminQuotePaginationResponse(result.getPage(), result.getSize(), result.isHasNext());
    response.content = result.getContent().stream().map(AdminQuoteResponse::from).toList();
    return response;
  }
}
