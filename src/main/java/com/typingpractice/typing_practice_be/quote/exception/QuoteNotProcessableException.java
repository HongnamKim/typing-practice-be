package com.typingpractice.typing_practice_be.quote.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class QuoteNotProcessableException extends BusinessException {
  public QuoteNotProcessableException() {
    super(ErrorCode.QUOTE_NOT_PROCESSABLE);
  }
}
