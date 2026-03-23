package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
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
  private QuoteLanguage language;
  private Float difficulty;
  private String author;
  private QuoteType type;
  private QuoteStatus status;
  private int reportCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  protected void fillFrom(Quote quote) {
    this.quoteId = quote.getId();
    this.sentence = quote.getSentence();
    this.language = quote.getLanguage();
    this.difficulty = quote.getDifficulty();
    this.author = quote.getAuthor();
    this.type = quote.getType();
    this.status = quote.getStatus();
    this.reportCount = quote.getReportCount();
    this.createdAt = quote.getCreatedAt();
    this.updatedAt = quote.getUpdatedAt();
  }

  public static QuoteResponse from(Quote quote) {
    QuoteResponse response = new QuoteResponse();

    response.fillFrom(quote);

    return response;
  }
}
