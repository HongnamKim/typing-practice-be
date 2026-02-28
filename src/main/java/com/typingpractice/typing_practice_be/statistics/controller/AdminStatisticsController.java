package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.statistics.service.GlobalQuoteStatisticsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.MemberTypingStatsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.QuoteTypingStatsBatchService;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class AdminStatisticsController {
  private final GlobalQuoteStatisticsBatchService globalQuoteStatisticsBatchService;
  private final QuoteTypingStatsBatchService quoteTypingStatsBatchService;
  private final MemberTypingStatsBatchService memberTypingStatsBatchService;

  @PostMapping("/global-quote/recalculate")
  public ApiResponse<Void> recalculate() {
    globalQuoteStatisticsBatchService.runManualRecalculation();
    return ApiResponse.ok(null);
  }

  @PostMapping("/quote-typing/recalculate")
  public ApiResponse<Void> recalculateQuoteTypingStats() {
    quoteTypingStatsBatchService.runManualRecalculation();
    return ApiResponse.ok(null);
  }

  @PostMapping("/member-typing/recalculate")
  public ApiResponse<Void> recalculateMemberTypingStats(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {

    if (startDate != null && endDate != null) {
      if (startDate.isAfter(endDate)) {
        throw new IllegalArgumentException("startDate는 endDate보다 같거나 이전이어야 합니다.");
      }
      memberTypingStatsBatchService.runRecalculationForPeriod(
          startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    } else {
      memberTypingStatsBatchService.runManualRecalculation();
    }

    return ApiResponse.ok(null);
  }
}
