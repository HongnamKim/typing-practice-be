package com.typingpractice.typing_practice_be.quote.dto;

import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.Range;

@Getter
@ToString
public class PublicQuoteRequest {
  @Range(min = 100, max = 300)
  private final Integer count;

  private final Boolean onlyMyQuotes;

  public PublicQuoteRequest(Integer count, Boolean onlyMyQuotes) {
    this.count = count != null ? count : 100;
    this.onlyMyQuotes = onlyMyQuotes != null ? onlyMyQuotes : false;
  }
}
