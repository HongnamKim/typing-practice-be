package com.typingpractice.typing_practice_be.report.query;

import com.typingpractice.typing_practice_be.report.dto.ReportProcessRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ReportProcessQuery {
  private final String sentence;
  private final String author;

  private ReportProcessQuery(String sentence, String author) {
    this.sentence = sentence;
    this.author = author;
  }

  public static ReportProcessQuery from(ReportProcessRequest request) {
    return new ReportProcessQuery(request.getSentence(), request.getAuthor());
  }
}
