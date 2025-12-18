package com.typingpractice.typing_practice_be.report.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class ReportNotProcessableException extends BusinessException {
  public ReportNotProcessableException() {
    super(ErrorCode.REPORT_NOT_PROCESSABLE);
  }
}
