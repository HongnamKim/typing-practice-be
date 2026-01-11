package com.typingpractice.typing_practice_be.auth.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class GoogleServerException extends BusinessException {
  public GoogleServerException() {
    super(ErrorCode.GOOGLE_SERVER_ERROR);
  }
}
