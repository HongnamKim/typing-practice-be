package com.typingpractice.typing_practice_be.adaptiveserving.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class AdaptiveQuoteListResponse {
  private int count;
  private List<AdaptiveQuoteResponse> content;

  public static AdaptiveQuoteListResponse of(List<AdaptiveQuoteResponse> content) {
    AdaptiveQuoteListResponse response = new AdaptiveQuoteListResponse();
    response.content = content;
    response.count = content.size();

    return response;
  }
}
