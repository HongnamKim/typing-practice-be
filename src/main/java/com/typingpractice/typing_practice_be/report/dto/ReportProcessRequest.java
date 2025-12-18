package com.typingpractice.typing_practice_be.report.dto;

import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Getter
@ToString
public class ReportProcessRequest {
  @Length(min = 5, max = 100)
  private String sentence;

  @Length(min = 1, max = 20)
  private String author;

  public static ReportProcessRequest create(String sentence, String author) {
    ReportProcessRequest request = new ReportProcessRequest();
    request.sentence = sentence;
    request.author = author;

    return request;
  }
}
