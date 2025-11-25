package com.typingpractice.typing_practice_be.member.exception;

import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicateEmailException extends RuntimeException {
  private final ErrorCode errorCode;

  public DuplicateEmailException() {
    super(ErrorCode.DUPLICATE_EMAIL.getMessage());
    this.errorCode = ErrorCode.DUPLICATE_EMAIL;
  }
}
