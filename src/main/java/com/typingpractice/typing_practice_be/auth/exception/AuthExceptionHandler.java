package com.typingpractice.typing_practice_be.auth.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.typingpractice.typing_practice_be.auth")
public class AuthExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleAuthException(BusinessException e) {
    log.warn("[Auth] {}: {}", e.getClass().getSimpleName(), e.getMessage());

    ErrorCode errorCode = e.getErrorCode();
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());

    pd.setTitle("Auth Domain Error");

    return pd;
  }
}
