package com.typingpractice.typing_practice_be.quote.exception;

import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class QuoteNotFoundException extends RuntimeException {
  private final ErrorCode errorCode;

  public QuoteNotFoundException() {
    super(ErrorCode.QUOTE_NOT_FOUND.getMessage());
    this.errorCode = ErrorCode.QUOTE_NOT_FOUND;
  }
}
