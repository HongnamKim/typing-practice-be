package com.typingpractice.typing_practice_be.common.exception;

import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.exception.ForbiddenException;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.View;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private final View error;

  public GlobalExceptionHandler(View error) {
    this.error = error;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationException(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("유효성 검사 실패");

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);

    problemDetail.setTitle("Validation Error");

    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
    String message = String.format("'%s'는 유효하지 않은 값입니다.", e.getValue());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    problemDetail.setTitle("Invalid Parameter Type");

    return problemDetail;
  }

  @ExceptionHandler(MemberNotFoundException.class)
  public ProblemDetail handleMemberNotFoundException(MemberNotFoundException e) {

    ErrorCode errorCode = e.getErrorCode();

    return ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ProblemDetail handleForbiddenException(ForbiddenException e) {
    ErrorCode errorCode = e.getErrorCode();

    return ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
  }

  @ExceptionHandler(QuoteNotFoundException.class)
  public ProblemDetail handleQuoteNotFoundException(QuoteNotFoundException e) {
    ErrorCode errorCode = e.getErrorCode();

    return ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
  }
}
