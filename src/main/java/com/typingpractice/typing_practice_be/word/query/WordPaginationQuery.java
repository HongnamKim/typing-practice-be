package com.typingpractice.typing_practice_be.word.query;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.query.PaginationQuery;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.domain.WordOrderBy;
import com.typingpractice.typing_practice_be.word.dto.request.WordPaginationRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class WordPaginationQuery extends PaginationQuery {

  private final WordLanguage language;
  private final WordOrderBy orderBy;

  private WordPaginationQuery(
      int page, int size, SortDirection sortDirection, WordLanguage language, WordOrderBy orderBy) {
    super(page, size, sortDirection);
    this.language = language;
    this.orderBy = orderBy;
  }

  public static WordPaginationQuery from(WordPaginationRequest request) {
    return new WordPaginationQuery(
        request.getPage(),
        request.getSize(),
        request.getSortDirection(),
        request.getLanguage(),
        request.getOrderBy());
  }
}
