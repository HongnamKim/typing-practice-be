package com.typingpractice.typing_practice_be.member.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class NotAdminException extends BusinessException {
  public NotAdminException() {
    super(ErrorCode.NOT_ADMIN);
  }
}
