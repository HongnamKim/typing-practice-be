package com.typingpractice.typing_practice_be.member.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class DuplicateNicknameException extends BusinessException {
  public DuplicateNicknameException() {
    super(ErrorCode.MEMBER_DUPLICATE_NICKNAME);
  }
}
