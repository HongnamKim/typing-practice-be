package com.typingpractice.typing_practice_be.quote.exception;

import com.typingpractice.typing_practice_be.common.exception.BusinessException;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class QuoteSimilarException extends BusinessException {
  private final String similarSentence;
  private final float similarity;

  public QuoteSimilarException(String similarSentence, float similarity) {
    super(ErrorCode.QUOTE_SIMILAR);
    this.similarSentence = similarSentence;
    this.similarity = similarity;
  }
}
