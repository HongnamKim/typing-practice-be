package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeletedQuoteResponse extends QuoteResponse {
  private LocalDateTime deletedAt;

  public static DeletedQuoteResponse from(Quote quote) {
    DeletedQuoteResponse response = new DeletedQuoteResponse();
    response.fillFrom(quote);
    response.deletedAt = quote.getDeletedAt();

    return response;
  }
}
