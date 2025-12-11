package com.typingpractice.typing_practice_be.member.exception;

import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class NotAdminException extends RuntimeException {
  private final ErrorCode errorCode;

  public NotAdminException() {
    super(ErrorCode.FORBIDDEN.getMessage());
    this.errorCode = ErrorCode.FORBIDDEN;
  }
}
