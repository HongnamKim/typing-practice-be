package com.typingpractice.typing_practice_be.statistics.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.service.YesterdayBatchStatusCache.Mode;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class YesterdayBatchStatusCacheTest {
  private static final Long MEMBER_ID = 1L;
  private static final String QUOTE_KO_KEY = "stats:yesterday-done:quote:1:KOREAN";

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOps;

  @InjectMocks private YesterdayBatchStatusCache cache;

  @Test
  @DisplayName("isDone: 마커 키가 있으면 true")
  void isDone_true_whenKeyExists() {
    when(redisTemplate.hasKey(QUOTE_KO_KEY)).thenReturn(true);

    assertThat(cache.isDone(Mode.QUOTE, MEMBER_ID, QuoteLanguage.KOREAN)).isTrue();
  }

  @Test
  @DisplayName("isDone: 마커 키가 없으면 false")
  void isDone_false_whenKeyMissing() {
    when(redisTemplate.hasKey(QUOTE_KO_KEY)).thenReturn(false);

    assertThat(cache.isDone(Mode.QUOTE, MEMBER_ID, QuoteLanguage.KOREAN)).isFalse();
  }

  @Test
  @DisplayName("markDone: 마커를 TTL과 함께 저장한다")
  void markDone_setsKeyWithTtl() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);

    cache.markDone(Mode.QUOTE, MEMBER_ID, QuoteLanguage.KOREAN);

    verify(valueOps).set(eq(QUOTE_KO_KEY), eq("1"), any(Duration.class));
  }

  @Test
  @DisplayName("clear: 주어진 모든 언어의 마커를 삭제한다")
  void clear_deletesAllLanguageKeys() {
    cache.clear(Mode.WORD, MEMBER_ID, WordLanguage.values());

    verify(redisTemplate)
        .delete(
            List.of(
                "stats:yesterday-done:word:1:KOREAN", "stats:yesterday-done:word:1:ENGLISH"));
  }
}
