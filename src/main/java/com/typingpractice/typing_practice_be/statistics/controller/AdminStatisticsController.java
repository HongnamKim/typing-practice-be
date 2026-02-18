package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.statistics.service.StatisticsBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class AdminStatisticsController {
  private final StatisticsBatchService batchService;

  @PostMapping("/recalculate")
  public ApiResponse<Void> recalculate() {
    batchService.runManualRecalculation();
    return ApiResponse.ok(null);
  }
}
