package com.typingpractice.typing_practice_be.word.dto.request;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.dto.PaginationRequest;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.domain.WordOrderBy;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class WordPaginationRequest extends PaginationRequest {
  private final WordLanguage language;
  private final WordOrderBy orderBy;

  public WordPaginationRequest(
      Integer page,
      Integer size,
      SortDirection sortDirection,
      WordLanguage language,
      WordOrderBy orderBy) {
    super(page, size, sortDirection);
    this.language = language;
    this.orderBy = orderBy != null ? orderBy : WordOrderBy.id;
  }
}
