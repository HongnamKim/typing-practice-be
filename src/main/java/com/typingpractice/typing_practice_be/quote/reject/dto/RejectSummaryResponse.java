package com.typingpractice.typing_practice_be.quote.reject.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class RejectSummaryResponse {
  private long rejectCount;
  private long totalUploadAttempts;
  private double rejectRate;
  private double avgSimilarity;

  public static RejectSummaryResponse create(
      long rejectCount, long totalUploadAttempts, double rejectRate, double avgSimilarity) {
    RejectSummaryResponse response = new RejectSummaryResponse();
    response.rejectCount = rejectCount;
    response.totalUploadAttempts = totalUploadAttempts;
    response.rejectRate = rejectRate;
    response.avgSimilarity = avgSimilarity;

    return response;
  }
}
