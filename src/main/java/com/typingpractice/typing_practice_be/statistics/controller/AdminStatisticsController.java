package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.quote.statistics.service.GlobalQuoteStatisticsBatchService;
import com.typingpractice.typing_practice_be.statistics.dto.MemberStatsDayRequest;
import com.typingpractice.typing_practice_be.statistics.dto.MemberStatsPeriodRequest;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.MemberDailyStatsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.MemberTypingStatsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.QuoteTypingStatsBatchService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class AdminStatisticsController {
  private final GlobalQuoteStatisticsBatchService globalQuoteStatisticsBatchService;
  private final QuoteTypingStatsBatchService quoteTypingStatsBatchService;
  private final MemberTypingStatsBatchService memberTypingStatsBatchService;
  private final MemberDailyStatsBatchService memberDailyStatsBatchService;

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
      @ModelAttribute @Valid MemberStatsPeriodRequest request) {

    LocalDate startDate = request.getStartDate();
    LocalDate endDate = request.getEndDate();
    String timezone = request.getTimezone();

    if (startDate != null && endDate != null) {
      ZoneId zone = TimeUtils.parseZoneId(timezone);

      memberTypingStatsBatchService.runRecalculationForPeriod(
          TimeUtils.startOfDayToUtc(startDate, zone), TimeUtils.endOfDayToUtc(endDate, zone));
    } else {
      memberTypingStatsBatchService.runManualRecalculation();
    }

    return ApiResponse.ok(null);
  }

  @PostMapping("/member-daily/recalculate")
  public ApiResponse<Void> recalculateMemberDailyStats(
      @ModelAttribute @Valid MemberStatsDayRequest request) {

    ZoneId zone = TimeUtils.parseZoneId(request.getTimezone());
    memberDailyStatsBatchService.runRecalculationForDate(request.getDate(), zone);

    return ApiResponse.ok(null);
  }
}
