package com.typingpractice.typing_practice_be.statistics.scheduler;

import com.typingpractice.typing_practice_be.statistics.service.StatisticsBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsScheduler {
  private final StatisticsBatchService batchService;

  @Scheduled(cron = "0 0 3 * * *")
  public void runDailyBatch() {
    log.info("전역 통계 배치 시작");
    batchService.runScheduledBatch();
    log.info("전역 통계 배치 완료");
  }
}
