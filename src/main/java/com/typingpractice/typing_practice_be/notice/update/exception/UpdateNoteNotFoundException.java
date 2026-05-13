package com.typingpractice.typing_practice_be.notice.update.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class UpdateNoteNotFoundException extends BusinessException {
  public UpdateNoteNotFoundException() {
    super(ErrorCode.UPDATE_NOTE_NOT_FOUND);
  }
}
