package com.typingpractice.typing_practice_be.word.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class WordInvalidCharacterException extends BusinessException {
  public WordInvalidCharacterException() {
    super(ErrorCode.WORD_INVALID_CHARACTER);
  }
}
