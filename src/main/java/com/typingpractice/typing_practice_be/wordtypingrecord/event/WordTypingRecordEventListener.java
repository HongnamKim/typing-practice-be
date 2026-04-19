package com.typingpractice.typing_practice_be.wordtypingrecord.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WordTypingRecordEventListener {
  @EventListener
  public void handleWordTypingRecordSaved(WordTypingRecordSavedEvent event) {
    try {
      // TodayWordTypingStatsRedisService.incrementTyping
      // TodayWordTypingStatsRedisService.incrementTypoAndDetail
      // AdaptiveServingRedisService.updateWordEstimation
    } catch (Exception e) {
      log.error("Redis 오늘 단어 통계 증분 실패 - memberId: {}", event.getMemberId());
    }
  }
}
