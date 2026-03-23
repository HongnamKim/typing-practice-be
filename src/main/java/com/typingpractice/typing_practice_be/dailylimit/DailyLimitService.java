package com.typingpractice.typing_practice_be.dailylimit;

public interface DailyLimitService {

  boolean tryIncrementQuoteUploadCount(Long memberId);

  boolean tryIncrementReportCount(Long memberId);
}
