package com.typingpractice.typing_practice_be.quote.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class QuoteLanguageMismatchException extends BusinessException {
  public QuoteLanguageMismatchException() {
    super(ErrorCode.QUOTE_LANGUAGE_MISMATCH);
  }
}
