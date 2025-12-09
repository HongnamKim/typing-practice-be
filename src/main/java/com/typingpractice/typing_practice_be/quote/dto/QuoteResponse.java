package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@ToString
public class QuoteResponse {
  private Long quoteId;
  private String sentence;
  private String author;
  private QuoteStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static QuoteResponse from(Quote quote) {
    QuoteResponse response = new QuoteResponse();

    response.quoteId = quote.getId();
    response.sentence = quote.getSentence();
    response.author = quote.getAuthor();
    response.status = quote.getStatus();
    response.createdAt = quote.getCreatedAt();
    response.updatedAt = quote.getUpdatedAt();

    return response;
  }
}
