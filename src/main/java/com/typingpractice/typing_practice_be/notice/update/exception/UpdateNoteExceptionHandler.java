package com.typingpractice.typing_practice_be.notice.update.exception;

import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.typingpractice.typing_practice_be.notice.update")
public class UpdateNoteExceptionHandler {
  private ProblemDetail createProblemDetail(ErrorCode errorCode) {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
    pd.setTitle("UpdateNote Domain Error");
    return pd;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDuplicateVersion(DataIntegrityViolationException e) {
    String message = e.getMessage();

    if (message != null && message.contains("uq_update_note_version")) {
      return createProblemDetail(ErrorCode.UPDATE_NOTE_VERSION_CONFLICT);
    }

    return createProblemDetail(ErrorCode.DATA_INTEGRITY_VIOLATION);
  }
}
