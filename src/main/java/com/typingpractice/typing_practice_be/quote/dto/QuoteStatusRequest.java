package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class QuoteStatusRequest {
  QuoteStatus status;
}
