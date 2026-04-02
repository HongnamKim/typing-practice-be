package com.typingpractice.typing_practice_be.statistics.scheduler;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.quote.service.difficulty.DifficultyBatchService;
import com.typingpractice.typing_practice_be.quote.statistics.service.GlobalQuoteStatisticsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.batch.MemberDailyStatsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.batch.MemberTypingStatsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.batch.MemberTypoStatsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.batch.QuoteTypingStatsBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsScheduler {
  private final GlobalQuoteStatisticsBatchService globalQuoteStatisticsBatchService;
  private final QuoteTypingStatsBatchService quoteTypingStatsBatchService;
  private final MemberTypingStatsBatchService memberTypingStatsBatchService;
  private final MemberDailyStatsBatchService memberDailyStatsBatchService;
  private final MemberTypoStatsBatchService memberTypoStatsBatchService;
  private final DifficultyBatchService difficultyBatchService;

  @Scheduled(cron = "0 0 3 * * *", zone = TimeUtils.KST_ZONE)
  public void runDailyBatch() {
    log.info("전역 통계 배치 시작");
    quoteTypingStatsBatchService.runScheduledBatch(); // 문장별 타이핑 통계
    globalQuoteStatisticsBatchService.runScheduledBatch(); // 전역 통계 (문장 특성 + 타이핑 성능)
    difficultyBatchService.runDynamicDifficultyBatch(); // 동적 난이도 보정
    memberTypingStatsBatchService.runScheduledBatch(); // 개인 타이핑 통계
    memberDailyStatsBatchService.runScheduledBatch(); // 개인 일간 통계
    memberTypoStatsBatchService.runScheduledBatch(); // 개인 오타 통계
    log.info("전역 통계 배치 완료");
  }
}
