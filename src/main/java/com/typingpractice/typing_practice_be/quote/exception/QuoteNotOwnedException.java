package com.typingpractice.typing_practice_be.quote.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class QuoteNotOwnedException extends BusinessException {

  public QuoteNotOwnedException() {
    super(ErrorCode.QUOTE_NOT_OWNED);
  }
}
