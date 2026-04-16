package com.typingpractice.typing_practice_be.word.scheduler;

import com.typingpractice.typing_practice_be.word.service.WordIdCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WordCacheScheduler {
  private final WordIdCacheService wordIdCacheService;

  @EventListener(ApplicationReadyEvent.class)
  public void initOnStartup() {
    log.info("[WordCacheScheduler] 앱 시작 후 단어 ID 캐시 초기화");
    wordIdCacheService.buildAllCaches();
  }
}
