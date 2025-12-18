package com.typingpractice.typing_practice_be.report.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class DuplicateReportException extends BusinessException {
  public DuplicateReportException() {
    super(ErrorCode.DUPLICATE_REPORT);
  }
}
