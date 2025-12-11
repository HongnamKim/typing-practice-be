package com.typingpractice.typing_practice_be.quote.dto;

import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Getter
@ToString
public class QuoteUpdateRequest {
  @Length(min = 5, max = 100)
  private String sentence;

  @Length(min = 1, max = 20)
  private String author;

  public static QuoteUpdateRequest create(String sentence, String author) {
    QuoteUpdateRequest request = new QuoteUpdateRequest();
    request.sentence = sentence;
    request.author = author;

    return request;
  }
}
