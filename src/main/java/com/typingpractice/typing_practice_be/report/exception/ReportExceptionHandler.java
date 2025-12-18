package com.typingpractice.typing_practice_be.report.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ReportExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleReportException(BusinessException e) {
    log.warn("[Report] {}: {}", e.getClass().getSimpleName(), e.getMessage());

    ErrorCode errorCode = e.getErrorCode();
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());

    pd.setTitle("Report Domain Error");

    return pd;
  }
}
