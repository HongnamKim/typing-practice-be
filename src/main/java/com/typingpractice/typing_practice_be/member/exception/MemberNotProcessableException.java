package com.typingpractice.typing_practice_be.member.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class MemberNotProcessableException extends BusinessException {
  public MemberNotProcessableException() {
    super(ErrorCode.MEMBER_NOT_PROCESSABLE);
  }
}
