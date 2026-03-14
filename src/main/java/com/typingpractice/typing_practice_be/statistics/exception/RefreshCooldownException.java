package com.typingpractice.typing_practice_be.statistics.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class RefreshCooldownException extends BusinessException {
  public RefreshCooldownException() {
    super(ErrorCode.REFRESH_COOLDOWN);
  }
}
