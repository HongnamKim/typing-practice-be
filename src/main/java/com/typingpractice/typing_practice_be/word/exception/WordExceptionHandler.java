package com.typingpractice.typing_practice_be.word.exception;

import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.typingpractice.typing_practice_be.word")
public class WordExceptionHandler {
  private ProblemDetail createProblemDetail(ErrorCode errorCode) {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());

    pd.setTitle("Word Domain Error");

    return pd;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDuplicateWordException(DataIntegrityViolationException e) {

    String message = e.getMessage();

    if (message != null && message.contains("uq_word")) {
      return createProblemDetail(ErrorCode.WORD_DUPLICATE);
    }

    return createProblemDetail(ErrorCode.DATA_INTEGRITY_VIOLATION);
  }
}
