package com.typingpractice.typing_practice_be.report.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;

public class QuoteNotReportableException extends BusinessException {
  public QuoteNotReportableException() {
    super(ErrorCode.QUOTE_NOT_REPORTABLE);
  }
}
