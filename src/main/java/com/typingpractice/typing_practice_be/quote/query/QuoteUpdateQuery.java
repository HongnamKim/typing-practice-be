package com.typingpractice.typing_practice_be.quote.query;

import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class QuoteUpdateQuery {
  private final String sentence;
  private final String author;

  private QuoteUpdateQuery(String sentence, String author) {
    this.sentence = sentence;
    this.author = author;
  }

  public static QuoteUpdateQuery from(QuoteUpdateRequest request) {
    return new QuoteUpdateQuery(request.getSentence(), request.getAuthor());
  }
}
