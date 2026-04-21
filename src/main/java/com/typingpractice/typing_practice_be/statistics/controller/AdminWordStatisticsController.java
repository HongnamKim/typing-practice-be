package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.statistics.dto.MemberStatsDayRequest;
import com.typingpractice.typing_practice_be.word.statistics.service.GlobalWordStatisticsBatchService;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service.MemberDailyWordStatsBatchService;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service.MemberWordTypingStatsBatchService;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service.WordTypingStatsBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class AdminWordStatisticsController {
  private final GlobalWordStatisticsBatchService globalWordStatisticsBatchService;
  private final WordTypingStatsBatchService wordTypingStatsBatchService;
  private final MemberWordTypingStatsBatchService memberWordTypingStatsBatchService;
  private final MemberDailyWordStatsBatchService memberDailyWordStatsBatchService;

  @PostMapping("/global-word/recalculate")
  public ApiResponse<Void> recalculateGlobalWordStats() {
    globalWordStatisticsBatchService.runManualRecalculation();
    return ApiResponse.ok(null);
  }

  @PostMapping("/word-typing/recalculate")
  public ApiResponse<Void> recalculateWordTypingStats() {
    wordTypingStatsBatchService.runManualRecalculation();
    return ApiResponse.ok(null);
  }

  @PostMapping("/member-word-typing/recalculate")
  public ApiResponse<Void> recalculateMemberWordTypingStats() {
    memberWordTypingStatsBatchService.runManualRecalculation();

    return ApiResponse.ok(null);
  }

  @PostMapping("/member-daily-word/recalculate")
  public ApiResponse<Void> recalculateMemberDailyWordStats(
      @RequestBody @Valid MemberStatsDayRequest request) {
    memberDailyWordStatsBatchService.runRecalculationForDate(request.getDate());

    return ApiResponse.ok(null);
  }
}
