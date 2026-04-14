package com.typingpractice.typing_practice_be.adaptiveserving.dto;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.typingrecord.domain.ServingType;
import lombok.Getter;

@Getter
public class AdaptiveQuoteResponse extends QuoteResponse {
  private ServingType servingType;

  public static AdaptiveQuoteResponse from(Quote quote, ServingType servingType) {
    AdaptiveQuoteResponse response = new AdaptiveQuoteResponse();
    response.fillFrom(quote);
    response.servingType = servingType;
    return response;
  }
}
