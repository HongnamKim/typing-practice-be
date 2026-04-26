package com.typingpractice.typing_practice_be.word.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class WordLanguageMismatchException extends BusinessException {
  public WordLanguageMismatchException() {
    super(ErrorCode.WORD_LANGUAGE_MISMATCH);
  }
}
