package com.typingpractice.typing_practice_be.auth.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class GoogleAuthException extends BusinessException {
  public GoogleAuthException() {
    super(ErrorCode.GOOGLE_AUTH_FAILED);
  }
}
