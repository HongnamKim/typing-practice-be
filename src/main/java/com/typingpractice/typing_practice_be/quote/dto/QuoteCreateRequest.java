package com.typingpractice.typing_practice_be.quote.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Getter
@ToString
public class QuoteCreateRequest {
  @NotEmpty
  @Length(min = 5, max = 100)
  private String sentence;

  @Length(min = 1, max = 20)
  private String author;

  public static QuoteCreateRequest create(String sentence, String author) {
    QuoteCreateRequest request = new QuoteCreateRequest();
    request.sentence = sentence;
    request.author = author;

    return request;
  }
}
