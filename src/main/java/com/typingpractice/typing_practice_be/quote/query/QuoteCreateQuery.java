package com.typingpractice.typing_practice_be.quote.query;

import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class QuoteCreateQuery {

  private final String sentence;
  private final String author;
  private final QuoteType type;

  private QuoteCreateQuery(String sentence, String author, QuoteType type) {

    this.sentence = sentence;
    this.author = author;
    this.type = type;
  }

  public static QuoteCreateQuery from(QuoteCreateRequest request) {
    return new QuoteCreateQuery(request.getSentence(), request.getAuthor(), request.getType());
  }
}
