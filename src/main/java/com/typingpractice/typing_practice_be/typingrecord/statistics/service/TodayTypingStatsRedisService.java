package com.typingpractice.typing_practice_be.typingrecord.statistics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.domain.Typo;
import com.typingpractice.typing_practice_be.typingrecord.event.TypingRecordSavedEvent;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypingAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypoAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.*;
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
  private static final String TYPO_DETAIL_KEY_PREFIX = "today:typo-detail:";

  private String typingKey(Long memberId, QuoteLanguage language) {
    // today:typing:1234:KOREAN
    return TYPING_KEY_PREFIX + memberId + ":" + language;
  }

  private String typoKey(Long memberId) {
    // today:typo:1234
    return TYPO_KEY_PREFIX + memberId;
  }

  private String typoDetailKey(Long memberId) {
    // today:typo-detail:1234
    return TYPO_DETAIL_KEY_PREFIX + memberId;
  }

  private Duration ttlUntilMidnightKst() {
    LocalDateTime nowKst = LocalDateTime.now(TimeUtils.KST);
    LocalDateTime midnightKst = LocalDateTime.of(nowKst.toLocalDate(), LocalTime.MAX);
    return Duration.between(nowKst, midnightKst);
  }

  // 이벤트 수신 후 오늘 통계 업데이트
  public void incrementTyping(TypingRecordSavedEvent event) {
    String key = typingKey(event.getMemberId(), event.getLanguage());

    TodayTypingSnapshot snapshot = getSnapshotOrEmpty(key);
    snapshot.increment(
        event.getCpm(), event.getAccuracy(), event.getCharLength(), event.getResetCount());

    setSnapshot(key, snapshot);
  }

  public void incrementTypoAndDetail(TypingRecordSavedEvent event) {
    if (event.isOutlier()) return;
    if (event.getTypos() == null || event.getTypos().isEmpty()) return;

    String typoKey = typoKey(event.getMemberId());
    String detailKey = typoDetailKey(event.getMemberId());

    TodayTypoSnapshot typoSnapshot = getTypoSnapshotOrEmpty(typoKey);

    for (Typo typo : event.getTypos()) {
      // Typo: language:expected 단위 카운트
      typoSnapshot.increment(event.getLanguage(), typo.getExpected());

      // TypoDetail: language:expected:actual 단위 카운트 (Hash)
      String field =
          TodayTypoDetailSnapshot.toKey(event.getLanguage(), typo.getExpected(), typo.getActual());
      TodayTypoDetailEntry entry = getTypoDetailEntry(detailKey, field);
      entry.increment(typo.getType());
      setTypoDetailEntry(detailKey, field, entry);
    }

    setTypoSnapshot(typoKey, typoSnapshot);
    ensureTtl(detailKey);
  }

  // 오늘 통계 조회
  public TodayTypingSnapshot getTyping(Long memberId, QuoteLanguage language) {
    String key = typingKey(memberId, language);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      return deserialize(json, TodayTypingSnapshot.class);
    }

    return fallbackTyping(memberId, language);
  }

  public TodayTypoSnapshot getTypo(Long memberId) {
    String key = typoKey(memberId);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      return deserialize(json, TodayTypoSnapshot.class);
    }

    return fallbackTypo(memberId);
  }

  public TodayTypoSnapshot getTypoByLanguage(Long memberId, QuoteLanguage language) {
    TodayTypoSnapshot snapshot = getTypo(memberId);
    String prefix = language + ":";
    Map<String, Integer> filtered = new HashMap<>();

    for (Map.Entry<String, Integer> entry : snapshot.getTypoCountMap().entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        filtered.put(entry.getKey(), entry.getValue());
      }
    }

    return TodayTypoSnapshot.create(filtered);
  }

  public TodayTypoDetailSnapshot getTypoDetail(Long memberId) {
    String key = typoDetailKey(memberId); // today:typo-detail:{memberId}
    Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

    if (!entries.isEmpty()) {
      Map<String, TodayTypoDetailEntry> map = new HashMap<>();
      for (Map.Entry<Object, Object> e : entries.entrySet()) {
        map.put(
            (String) e.getKey(), deserialize((String) e.getValue(), TodayTypoDetailEntry.class));
      }

      return TodayTypoDetailSnapshot.create(map);
    }

    return fallbackTypoDetail(memberId);
  }

  public void invalidateTyping(Long memberId, QuoteLanguage language) {
    redisTemplate.delete(typingKey(memberId, language));
  }

  public void invalidateTypo(Long memberId) {
    redisTemplate.delete(typoKey(memberId));
  }

  public void invalidateTypoDetail(Long memberId) {
    redisTemplate.delete(typoDetailKey(memberId));
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

  private TodayTypoDetailEntry getTypoDetailEntry(String hashKey, String field) {
    Object value = redisTemplate.opsForHash().get(hashKey, field);
    if (value != null) {
      return deserialize((String) value, TodayTypoDetailEntry.class);
    }

    return TodayTypoDetailEntry.empty();
  }

  private void setSnapshot(String key, TodayTypingSnapshot snapshot) {
    String json = serialize(snapshot);
    redisTemplate.opsForValue().set(key, json, ttlUntilMidnightKst());
  }

  private void setTypoSnapshot(String key, TodayTypoSnapshot snapshot) {
    String json = serialize(snapshot);
    redisTemplate.opsForValue().set(key, json, ttlUntilMidnightKst());
  }

  private void setTypoDetailEntry(String hashKey, String field, TodayTypoDetailEntry entry) {
    redisTemplate.opsForHash().put(hashKey, field, serialize(entry));
  }

  private void ensureTtl(String key) {
    Long ttl = redisTemplate.getExpire(key);
    if (ttl == null || ttl < 0) {
      redisTemplate.expire(key, ttlUntilMidnightKst());
    }
  }

  private TodayTypingSnapshot fallbackTyping(Long memberId, QuoteLanguage language) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(todayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(todayKst);

    List<MemberTypingAggregation> results =
        memberTypingAggregationRepository.aggregateByMemberIdsAndLanguageBetween(
            List.of(memberId), language, from, to);

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

    setSnapshot(typingKey(memberId, language), snapshot);
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

    Map<String, Integer> typoMap = new HashMap<>();

    for (MemberTypoAggregation agg : results) {
      // Typo
      String typoKey = TodayTypoSnapshot.toKey(agg.getLanguage(), agg.getExpected());
      typoMap.merge(typoKey, agg.getCount(), Integer::sum);
    }

    TodayTypoSnapshot snapshot = TodayTypoSnapshot.create(typoMap);
    setTypoSnapshot(typoKey(memberId), snapshot);

    return snapshot;
  }

  private TodayTypoDetailSnapshot fallbackTypoDetail(Long memberId) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(todayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(todayKst);

    List<MemberTypoAggregation> results =
        memberTypoAggregationRepository.aggregateByMemberIdsBetween(List.of(memberId), from, to);

    if (results.isEmpty()) {
      return TodayTypoDetailSnapshot.empty();
    }

    String hashKey = typoDetailKey(memberId);
    Map<String, TodayTypoDetailEntry> map = new HashMap<>();

    for (MemberTypoAggregation agg : results) {
      String field =
          TodayTypoDetailSnapshot.toKey(agg.getLanguage(), agg.getExpected(), agg.getActual());
      TodayTypoDetailEntry entry =
          TodayTypoDetailEntry.create(
              agg.getCount(),
              agg.getInitialCount(),
              agg.getMedialCount(),
              agg.getFinalCount(),
              agg.getLetterCount());
      setTypoDetailEntry(hashKey, field, entry);
      map.put(field, entry);
    }

    ensureTtl(hashKey);
    return TodayTypoDetailSnapshot.create(map);
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
