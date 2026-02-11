package com.typingpractice.typing_practice_be.auth.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.typingpractice.typing_practice_be.auth")
public class AuthExceptionHandler {

  private ProblemDetail createProblemDetail(ErrorCode errorCode) {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());

    pd.setTitle("Auth Domain Error");

    return pd;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDuplicateMemberException(DataIntegrityViolationException e) {
    String message = e.getMessage();

    if (message != null && message.contains("provider_id")) {
      return createProblemDetail(ErrorCode.AUTH_DUPLICATE_USER);
    }

    return createProblemDetail(ErrorCode.DATA_INTEGRITY_VIOLATION);
  }

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
