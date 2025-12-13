package com.typingpractice.typing_practice_be.member.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class MemberNotFoundException extends BusinessException {
  public MemberNotFoundException() {
    super(ErrorCode.MEMBER_NOT_FOUND);
  }
}
