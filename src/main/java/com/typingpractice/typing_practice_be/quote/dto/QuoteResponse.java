package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import java.time.LocalDateTime;

import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
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

  private float avgCpm;
  private float avgAcc;

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

    if (quote.getTypingStats() != null) {
      QuoteTypingStats typingStats = quote.getTypingStats();

      this.avgAcc = typingStats.getAvgAcc();
      this.avgCpm = typingStats.getAvgCpm();
    }
  }

  public static QuoteResponse from(Quote quote) {
    QuoteResponse response = new QuoteResponse();

    response.fillFrom(quote);

    return response;
  }
}
