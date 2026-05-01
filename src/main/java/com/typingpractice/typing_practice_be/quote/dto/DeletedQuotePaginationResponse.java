package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.common.dto.PaginationResponse;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import java.util.List;
import lombok.Getter;

@Getter
public class DeletedQuotePaginationResponse extends PaginationResponse {
  private List<DeletedQuoteResponse> content;

  protected DeletedQuotePaginationResponse(int page, int size, boolean hasNext) {
    super(page, size, hasNext);
  }

  public static DeletedQuotePaginationResponse from(PageResult<Quote> result) {
    DeletedQuotePaginationResponse response =
        new DeletedQuotePaginationResponse(result.getPage(), result.getSize(), result.isHasNext());

    response.content = result.getContent().stream().map(DeletedQuoteResponse::from).toList();
    return response;
  }
}
