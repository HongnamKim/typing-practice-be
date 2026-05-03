package com.typingpractice.typing_practice_be.notice.announcement.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class AnnouncementNotFoundException extends BusinessException {
  public AnnouncementNotFoundException() {
    super(ErrorCode.ANNOUNCEMENT_NOT_FOUND);
  }
}
