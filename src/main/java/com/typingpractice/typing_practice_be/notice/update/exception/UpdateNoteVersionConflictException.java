package com.typingpractice.typing_practice_be.notice.update.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class UpdateNoteVersionConflictException extends BusinessException {
  public UpdateNoteVersionConflictException() {
    super(ErrorCode.UPDATE_NOTE_VERSION_CONFLICT);
  }
}
