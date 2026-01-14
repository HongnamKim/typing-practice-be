package com.typingpractice.typing_practice_be.auth.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class AuthInvalidRefreshTokenException extends BusinessException {
  public AuthInvalidRefreshTokenException() {
    super(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
  }
}
