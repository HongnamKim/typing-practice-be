package com.typingpractice.typing_practice_be.quote.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class QuoteDuplicateException extends BusinessException {
  public QuoteDuplicateException() {
    super(ErrorCode.QUOTE_DUPLICATE);
  }
}
