package com.typingpractice.typing_practice_be.dailylimit;

public interface DailyLimitService {
  boolean canReport(Long memberId);

  void incrementReportCount(Long memberId);
}
