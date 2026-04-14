package com.typingpractice.typing_practice_be.typingrecord.event;

import com.typingpractice.typing_practice_be.adaptiveserving.service.AdaptiveServingRedisService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.TodayTypingStatsRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TypingRecordEventListener {
  private final TodayTypingStatsRedisService todayTypingStatsRedisService;
  private final AdaptiveServingRedisService adaptiveServingRedisService;

  @EventListener
  public void handleTypingRecordSaved(TypingRecordSavedEvent event) {
    try {
      todayTypingStatsRedisService.incrementTyping(event);
      todayTypingStatsRedisService.incrementTypoAndDetail(event);

      adaptiveServingRedisService.updateEstimation(event);
    } catch (Exception e) {
      log.error("Redis 오늘 통계 증분 실패 - memberId: {}", event.getMemberId());
    }
  }
}
