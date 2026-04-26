package com.typingpractice.typing_practice_be.word.dto.response;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.common.dto.PaginationResponse;
import com.typingpractice.typing_practice_be.word.domain.Word;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminWordPaginationResponse extends PaginationResponse {
  private List<AdminWordResponse> content;

  protected AdminWordPaginationResponse(int page, int size, boolean hasNext) {
    super(page, size, hasNext);
  }

  public static AdminWordPaginationResponse from(PageResult<Word> result) {
    AdminWordPaginationResponse response =
        new AdminWordPaginationResponse(result.getPage(), result.getSize(), result.isHasNext());
    response.content = result.getContent().stream().map(AdminWordResponse::from).toList();
    return response;
  }
}
