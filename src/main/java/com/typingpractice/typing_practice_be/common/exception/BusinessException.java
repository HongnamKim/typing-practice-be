package com.typingpractice.typing_practice_be.common.exception;

import lombok.Getter;

@Getter
public abstract class BusinessException extends RuntimeException {
  protected ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
