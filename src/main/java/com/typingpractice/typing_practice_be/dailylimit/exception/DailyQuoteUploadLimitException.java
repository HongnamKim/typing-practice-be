package com.typingpractice.typing_practice_be.dailylimit.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class DailyQuoteUploadLimitException extends BusinessException {
  public DailyQuoteUploadLimitException() {
    super(ErrorCode.DAILY_QUOTE_UPLOAD_LIMIT);
  }
}
