package com.typingpractice.typing_practice_be.typingrecord.statistics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.typingrecord.domain.Typo;
import com.typingpractice.typing_practice_be.typingrecord.event.TypingRecordSavedEvent;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypingAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypoAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypingAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypingSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodayTypingStatsRedisService {
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final MemberTypingAggregationRepository memberTypingAggregationRepository;
  private final MemberTypoAggregationRepository memberTypoAggregationRepository;

  private static final String TYPING_KEY_PREFIX = "today:typing:";
  private static final String TYPO_KEY_PREFIX = "today:typo:";

  private String typingKey(Long memberId) {
    // today:typing:1234
    return TYPING_KEY_PREFIX + memberId;
  }

  private String typoKey(Long memberId) {
    // today:typo:1234
    return TYPO_KEY_PREFIX + memberId;
  }

  private Duration ttlUntilMidnightKst() {
    LocalDateTime nowKst = LocalDateTime.now(TimeUtils.KST);
    LocalDateTime midnightKst = LocalDateTime.of(nowKst.toLocalDate(), LocalTime.MAX);
    return Duration.between(nowKst, midnightKst);
  }

  // 이벤트 수신 후 오늘 통계 업데이트
  public void incrementTyping(TypingRecordSavedEvent event) {
    String key = typingKey(event.getMemberId());

    TodayTypingSnapshot snapshot = getSnapshotOrEmpty(key);
    snapshot.increment(
        event.getCpm(), event.getAccuracy(), event.getCharLength(), event.getResetCount());

    setSnapshot(key, snapshot);
  }

  public void incrementTypo(TypingRecordSavedEvent event) {
    if (event.isOutlier()) return;
    if (event.getTypos() == null || event.getTypos().isEmpty()) return;

    String key = typoKey(event.getMemberId());

    TodayTypoSnapshot snapshot = getTypoSnapshotOrEmpty(key);
    for (Typo typo : event.getTypos()) {
      snapshot.increment(event.getLanguage(), typo.getExpected());
    }

    setTypoSnapshot(key, snapshot);
  }

  // 오늘 통계 조회
  public TodayTypingSnapshot getTyping(Long memberId) {
    String key = typingKey(memberId);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      return deserialize(json, TodayTypingSnapshot.class);
    }

    return fallbackTyping(memberId);
  }

  public TodayTypoSnapshot getTypo(Long memberId) {
    String key = typoKey(memberId);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      return deserialize(json, TodayTypoSnapshot.class);
    }

    return fallbackTypo(memberId);
  }

  public void invalidateTyping(Long memberId) {
    redisTemplate.delete(typingKey(memberId));
  }

  public void invalidateTypo(Long memberId) {
    redisTemplate.delete(typoKey(memberId));
  }

  private TodayTypingSnapshot getSnapshotOrEmpty(String key) {
    String json = redisTemplate.opsForValue().get(key);
    if (json != null) {
      return deserialize(json, TodayTypingSnapshot.class);
    }
    return TodayTypingSnapshot.empty();
  }

  private TodayTypoSnapshot getTypoSnapshotOrEmpty(String key) {
    String json = redisTemplate.opsForValue().get(key);
    if (json != null) {
      return deserialize(json, TodayTypoSnapshot.class);
    }

    return TodayTypoSnapshot.empty();
  }

  private void setSnapshot(String key, TodayTypingSnapshot snapshot) {
    String json = serialize(snapshot);
    redisTemplate.opsForValue().set(key, json, ttlUntilMidnightKst());
  }

  private void setTypoSnapshot(String key, TodayTypoSnapshot snapshot) {
    String json = serialize(snapshot);
    redisTemplate.opsForValue().set(key, json, ttlUntilMidnightKst());
  }

  private TodayTypingSnapshot fallbackTyping(Long memberId) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(todayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(todayKst);

    List<MemberTypingAggregation> results =
        memberTypingAggregationRepository.aggregateByMemberIdsBetween(List.of(memberId), from, to);

    if (results.isEmpty()) {
      return TodayTypingSnapshot.empty();
    }

    MemberTypingAggregation agg = results.getFirst();
    TodayTypingSnapshot snapshot =
        TodayTypingSnapshot.create(
            agg.getTotalAttempts(),
            agg.getAvgCpm(),
            agg.getAvgAcc(),
            agg.getBestCpm(),
            agg.getTotalPracticeTimeMin(),
            agg.getTotalResetCount());

    setSnapshot(typingKey(memberId), snapshot);
    return snapshot;
  }

  private TodayTypoSnapshot fallbackTypo(Long memberId) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(todayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(todayKst);

    List<MemberTypoAggregation> results =
        memberTypoAggregationRepository.aggregateByMemberIdsBetween(List.of(memberId), from, to);

    if (results.isEmpty()) {
      return TodayTypoSnapshot.empty();
    }

    Map<String, Integer> map = new HashMap<>();
    for (MemberTypoAggregation agg : results) {
      String key = TodayTypoSnapshot.toKey(agg.getLanguage(), agg.getExpected());
      map.merge(key, agg.getCount(), Integer::sum);
    }

    TodayTypoSnapshot snapshot = TodayTypoSnapshot.create(map);
    setTypoSnapshot(typoKey(memberId), snapshot);

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
