package com.typingpractice.typing_practice_be.member.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.typingpractice.typing_practice_be.member")
public class MemberExceptionHandler {
  private ProblemDetail createProblemDetail(ErrorCode errorCode) {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());

    pd.setTitle("Member Domain Error");

    return pd;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDuplicateNicknameException(DataIntegrityViolationException e) {
    String message = e.getMessage();

    if (message != null && message.contains("nickname")) {
      return createProblemDetail(ErrorCode.MEMBER_DUPLICATE_NICKNAME);
    }

    return createProblemDetail(ErrorCode.DATA_INTEGRITY_VIOLATION);
  }

  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleMemberException(BusinessException e) {
    log.warn("[Member] {}: {}", e.getClass().getSimpleName(), e.getMessage());

    ErrorCode errorCode = e.getErrorCode();
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());

    pd.setTitle("Member Domain Error");

    return pd;
  }
}
