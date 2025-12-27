package com.typingpractice.typing_practice_be.dailylimit.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class DailyReportLimitException extends BusinessException {
  public DailyReportLimitException() {
    super(ErrorCode.DAILY_REPORT_LIMIT);
  }
}
