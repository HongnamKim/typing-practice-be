package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.event.WordTypingRecordSavedEvent;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypingAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.MemberWordTypingAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypingSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodayWordTypingStatsRedisService {
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final MemberWordTypingAggregationRepository aggregationRepository;

  private static final String TYPING_KEY_PREFIX = "today:word-typing:";

  private String typingKey(Long memberId, WordLanguage language) {
    return TYPING_KEY_PREFIX + memberId + ":" + language;
  }

  private Duration ttlUntilMidnightKst() {
    LocalDateTime nowKst = LocalDateTime.now(TimeUtils.KST);
    LocalDateTime midnightKst = LocalDateTime.of(nowKst.toLocalDate(), LocalTime.MAX);
    return Duration.between(nowKst, midnightKst);
  }

  public void invalidateAll(Long memberId) {
    for (WordLanguage lang : WordLanguage.values()) {
      invalidateTyping(memberId, lang);
    }
  }

  public void invalidateTyping(Long memberId, WordLanguage language) {
    redisTemplate.delete(typingKey(memberId, language));
  }

  public void incrementTyping(WordTypingRecordSavedEvent event) {
    if (event.isOutlier()) return;

    String key = typingKey(event.getMemberId(), event.getLanguage());
    TodayWordTypingSnapshot snapshot = getSnapshotOrEmpty(key);
    snapshot.increment(
        event.getWpm(), event.getAccuracy(), event.getWordCount(), event.getElapsedTimeMs());
    setSnapshot(key, snapshot);
  }

  public TodayWordTypingSnapshot getTyping(Long memberId, WordLanguage language) {
    String key = typingKey(memberId, language);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      return deserialize(json, TodayWordTypingSnapshot.class);
    }

    return fallbackTyping(memberId, language);
  }

  private TodayWordTypingSnapshot getSnapshotOrEmpty(String key) {
    String json = redisTemplate.opsForValue().get(key);
    if (json != null) {
      return deserialize(json, TodayWordTypingSnapshot.class);
    }
    return TodayWordTypingSnapshot.empty();
  }

  private void setSnapshot(String key, TodayWordTypingSnapshot snapshot) {
    String json = serialize(snapshot);
    redisTemplate.opsForValue().set(key, json, ttlUntilMidnightKst());
  }

  private TodayWordTypingSnapshot fallbackTyping(Long memberId, WordLanguage language) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(todayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(todayKst);

    List<MemberWordTypingAggregation> results =
        aggregationRepository.aggregateByMemberIdsAndLanguageBetween(
            List.of(memberId), language, from, to);

    if (results.isEmpty()) {
      return TodayWordTypingSnapshot.empty();
    }

    MemberWordTypingAggregation agg = results.getFirst();
    TodayWordTypingSnapshot snapshot =
        TodayWordTypingSnapshot.create(
            agg.getTotalAttempts(),
            agg.getTotalWordsAttempted(),
            agg.getAvgWpm(),
            agg.getAvgAcc(),
            agg.getBestWpm(),
            agg.getTotalPracticeTimeMin());

    setSnapshot(typingKey(memberId, language), snapshot);
    return snapshot;
  }

  private String serialize(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Redis 직렬화 실패", e);
    }
  }

  private <T> T deserialize(String json, Class<T> clazz) {
    try {
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Redis 역직렬화 실패", e);
    }
  }
}
