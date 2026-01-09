package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.common.dto.PaginationResponse;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class QuotePaginationResponse extends PaginationResponse {
  private List<QuoteResponse> content;

  protected QuotePaginationResponse(int page, int size, boolean hasNext) {
    super(page, size, hasNext);
  }

  public static QuotePaginationResponse from(PageResult<Quote> result) {
    QuotePaginationResponse response =
        new QuotePaginationResponse(result.getPage(), result.getSize(), result.isHasNext());

    response.content = result.getContent().stream().map(QuoteResponse::from).toList();

    return response;
  }
}
