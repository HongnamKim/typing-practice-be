package com.typingpractice.typing_practice_be.quote.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class EmptyUpdateRequestException extends BusinessException {
  public EmptyUpdateRequestException() {
    super(ErrorCode.EMPTY_UPDATE_REQUEST);
  }
}
