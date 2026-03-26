package com.typingpractice.typing_practice_be.quote.scheduler;

import com.typingpractice.typing_practice_be.quote.service.QuoteIdCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteIdCacheScheduler {
	private final QuoteIdCacheService quoteIdCacheService;

	private static final int REFRESH_PERIOD = 5 * 60 * 1000;

	@EventListener(ApplicationReadyEvent.class)
	public void initOnStartup() {
		log.info("[QuoteIdCacheScheduler] 앱 시작 후 공개 문장 ID 캐시 초기화");
		quoteIdCacheService.refreshAllPublicIds();
	}

	@Scheduled(fixedDelay = REFRESH_PERIOD, initialDelay = REFRESH_PERIOD)
	public void refreshPublicIds() {
		log.info("[QuoteIdCacheScheduler] 공개 문장 ID 캐시 갱신 시작");
		quoteIdCacheService.refreshAllPublicIds();
		log.info("[QuoteIdCacheScheduler] 공개 문장 ID 캐시 갱신 완료");
	}
}
