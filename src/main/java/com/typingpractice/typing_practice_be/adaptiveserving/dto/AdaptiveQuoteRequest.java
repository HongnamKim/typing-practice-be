package com.typingpractice.typing_practice_be.adaptiveserving.dto;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import java.util.List;
import lombok.Getter;
import org.hibernate.validator.constraints.Range;

@Getter
public class AdaptiveQuoteRequest {
  private final QuoteLanguage language;

  @Range(min = 50, max = 100)
  private final int count;

  private final List<Long> excludeIds;

  public AdaptiveQuoteRequest(QuoteLanguage language, Integer count, List<Long> excludeIds) {
    this.language = language;
    this.count = count != null ? count : 50;
    this.excludeIds = excludeIds;
  }
}
