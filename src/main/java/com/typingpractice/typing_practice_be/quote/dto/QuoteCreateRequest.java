package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
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

  private QuoteType type = QuoteType.PUBLIC;

  public static QuoteCreateRequest create(String sentence, String author, QuoteType type) {
    QuoteCreateRequest request = new QuoteCreateRequest();
    request.sentence = sentence;
    request.author = author;
    request.type = type;

    return request;
  }
}
