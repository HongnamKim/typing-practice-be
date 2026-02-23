package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.statistics.service.GlobalQuoteStatisticsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.QuoteTypingStatsBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class AdminStatisticsController {
  private final GlobalQuoteStatisticsBatchService globalQuoteStatisticsBatchService;
  private final QuoteTypingStatsBatchService quoteTypingStatsBatchService;

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
}
