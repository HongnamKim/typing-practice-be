package com.typingpractice.typing_practice_be.quote.query;

import com.typingpractice.typing_practice_be.quote.dto.PublicQuoteRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PublicQuoteQuery {
  private final Integer page;
  private final Integer count;
  private final Boolean onlyMyQuotes;
  private final Long memberId;
  private final Float seed;

  private PublicQuoteQuery(
      Long memberId, Integer page, Integer count, Boolean onlyMyQuotes, Float seed) {
    this.page = page;
    this.count = count;
    this.onlyMyQuotes = onlyMyQuotes;
    this.memberId = memberId;
    this.seed = seed;
  }

  public static PublicQuoteQuery from(Long memberId, PublicQuoteRequest request) {

    return new PublicQuoteQuery(
        memberId,
        request.getPage(),
        request.getCount(),
        request.getOnlyMyQuotes(),
        request.getSeed());
  }
}
