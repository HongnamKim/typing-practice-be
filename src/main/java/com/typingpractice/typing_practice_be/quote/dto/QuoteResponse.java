package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class QuoteResponse {
  private Long quoteId;
  private String sentence;
  private String author;
  private QuoteType type;
  private QuoteStatus status;
  private int reportCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static QuoteResponse from(Quote quote) {
    QuoteResponse response = new QuoteResponse();

    response.quoteId = quote.getId();
    response.sentence = quote.getSentence();
    response.author = quote.getAuthor();
    response.type = quote.getType();
    response.status = quote.getStatus();
    response.reportCount = quote.getReportCount();
    response.createdAt = quote.getCreatedAt();
    response.updatedAt = quote.getUpdatedAt();

    return response;
  }
}
