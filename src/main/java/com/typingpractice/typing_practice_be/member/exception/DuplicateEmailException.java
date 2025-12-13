package com.typingpractice.typing_practice_be.member.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class DuplicateEmailException extends BusinessException {
  public DuplicateEmailException() {
    super(ErrorCode.DUPLICATE_EMAIL);
  }
}
