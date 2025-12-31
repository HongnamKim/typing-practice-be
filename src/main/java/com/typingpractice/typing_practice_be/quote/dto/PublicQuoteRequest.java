package com.typingpractice.typing_practice_be.quote.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.Range;

@Getter
@ToString
public class PublicQuoteRequest {
  @Min(value = 1)
  private final Integer page;

  @Range(min = 100, max = 300)
  private final Integer count;

  private final Boolean onlyMyQuotes;

  @NotNull
  @DecimalMin("-1.0")
  @DecimalMax("1.0")
  private final Float seed;

  public PublicQuoteRequest(Integer page, Integer count, Boolean onlyMyQuotes, Float seed) {
    this.page = page != null ? page : 1;
    this.count = count != null ? count : 100;
    this.onlyMyQuotes = onlyMyQuotes != null ? onlyMyQuotes : false;
    this.seed = seed;
    // this.seed = seed != null ? seed : (float) (Math.random() * 2 - 1);
  }
}
