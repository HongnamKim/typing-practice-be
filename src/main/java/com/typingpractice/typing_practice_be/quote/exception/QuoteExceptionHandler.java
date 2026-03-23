package com.typingpractice.typing_practice_be.quote.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.typingpractice.typing_practice_be.quote")
public class QuoteExceptionHandler {
  private final String QUOTE_DOMAIN_ERROR_TITLE = "Quote Domain Error";

  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleQuoteException(BusinessException e) {
    log.warn("[Quote] {}: {}", e.getClass().getSimpleName(), e.getMessage());

    ErrorCode errorCode = e.getErrorCode();
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());

    pd.setTitle(QUOTE_DOMAIN_ERROR_TITLE);

    return pd;
  }

  @ExceptionHandler(QuoteSimilarException.class)
  public ProblemDetail handleQuoteSimilarException(QuoteSimilarException e) {
    log.warn("[Quote] 유사 문장 감지: similarity={}", e.getSimilarity());

    ErrorCode errorCode = e.getErrorCode();
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());

    pd.setTitle(QUOTE_DOMAIN_ERROR_TITLE);
    pd.setProperty("similarSentence", e.getSimilarSentence());
    pd.setProperty("similarity", e.getSimilarity());

    return pd;
  }
}
