package com.typingpractice.typing_practice_be.word.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class WordNotFoundException extends BusinessException {
  public WordNotFoundException() {
    super(ErrorCode.WORD_NOT_FOUND);
  }
}
