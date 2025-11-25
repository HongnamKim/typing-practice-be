package com.typingpractice.typing_practice_be.member.exception;

import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class MemberNotFoundException extends RuntimeException {
  private final ErrorCode errorCode;

  public MemberNotFoundException() {
    super(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    this.errorCode = ErrorCode.MEMBER_NOT_FOUND;
  }
}
