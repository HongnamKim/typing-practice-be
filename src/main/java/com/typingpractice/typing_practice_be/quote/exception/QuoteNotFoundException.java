package com.typingpractice.typing_practice_be.quote.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class QuoteNotFoundException extends BusinessException {
  public QuoteNotFoundException() {
    super(ErrorCode.QUOTE_NOT_FOUND);
  }
}
