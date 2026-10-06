package com.typingpractice.typing_practice_be.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

// "어제 배치 완료" 또는 "어제 기록 없음" 판정을 자정(KST)까지 캐싱한다.
// 배치 미완료(pending)는 같은 날 03시 배치 이후 완료로 바뀌므로 캐싱하지 않는다.
// (stale 한 pending 이 남으면 PG 의 어제 통계와 Mongo 어제 집계가 중복 반영됨)
@Component
@RequiredArgsConstructor
public class YesterdayBatchStatusCache {
  private final StringRedisTemplate redisTemplate;

  private static final String KEY_PREFIX = "stats:yesterday-done:";

  public enum Mode {
    QUOTE,
    WORD
  }

  private String key(Mode mode, Long memberId, Enum<?> language) {
    // stats:yesterday-done:quote:1234:KOREAN
    return KEY_PREFIX + mode.name().toLowerCase() + ":" + memberId + ":" + language.name();
  }

  private Duration ttlUntilMidnightKst() {
    LocalDateTime nowKst = LocalDateTime.now(TimeUtils.KST);
    LocalDateTime midnightKst = LocalDateTime.of(nowKst.toLocalDate(), LocalTime.MAX);
    return Duration.between(nowKst, midnightKst);
  }

  public boolean isDone(Mode mode, Long memberId, Enum<?> language) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(key(mode, memberId, language)));
  }

  public void markDone(Mode mode, Long memberId, Enum<?> language) {
    redisTemplate.opsForValue().set(key(mode, memberId, language), "1", ttlUntilMidnightKst());
  }

  public void clear(Mode mode, Long memberId, Enum<?>[] languages) {
    List<String> keys = Arrays.stream(languages).map(lang -> key(mode, memberId, lang)).toList();
    redisTemplate.delete(keys);
  }
}
