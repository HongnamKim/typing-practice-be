package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordDetail;
import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypo;
import com.typingpractice.typing_practice_be.wordtypingrecord.event.WordTypingRecordSavedEvent;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypingAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypoAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailEntry;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypingAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypingSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
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
public class TodayWordTypingStatsRedisService {
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final MemberWordTypingAggregationRepository aggregationRepository;
  private final MemberWordTypoAggregationRepository typoAggregationRepository;

  private static final String TYPING_KEY_PREFIX = "today:word-typing:";
  private static final String TYPO_KEY_PREFIX = "today:word-typo:";
  private static final String TYPO_DETAIL_KEY_PREFIX = "today:word-typo-detail:";

  private String typingKey(Long memberId, WordLanguage language) {
    return TYPING_KEY_PREFIX + memberId + ":" + language;
  }

  private String typoKey(Long memberId) {
    return TYPO_KEY_PREFIX + memberId;
  }

  private String typoDetailKey(Long memberId) {
    return TYPO_DETAIL_KEY_PREFIX + memberId;
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
    invalidateTypo(memberId);
    invalidateTypoDetail(memberId);
  }

  public void invalidateTyping(Long memberId, WordLanguage language) {
    redisTemplate.delete(typingKey(memberId, language));
  }

  public void invalidateTypo(Long memberId) {
    redisTemplate.delete(typoKey(memberId));
  }

  public void invalidateTypoDetail(Long memberId) {
    redisTemplate.delete(typoDetailKey(memberId));
  }

  public void incrementTyping(WordTypingRecordSavedEvent event) {
    if (event.isOutlier()) return;

    String key = typingKey(event.getMemberId(), event.getLanguage());
    TodayWordTypingSnapshot snapshot = getSnapshotOrEmpty(key);
    snapshot.increment(
        event.getWpm(), event.getAccuracy(), event.getWordCount(), event.getElapsedTimeMs());
    setSnapshot(key, snapshot);
  }

  public void incrementTypoAndDetail(WordTypingRecordSavedEvent event) {
    if (event.isOutlier()) return;
    if (event.getWordDetails() == null || event.getWordDetails().isEmpty()) return;

    String typoKey = typoKey(event.getMemberId());
    String detailKey = typoDetailKey(event.getMemberId());

    TodayWordTypoSnapshot typoSnapshot = getTypoSnapshotOrEmpty(typoKey);

    for (WordDetail wordDetail : event.getWordDetails()) {
      if (wordDetail.getTypos() == null) continue;

      for (WordTypo typo : wordDetail.getTypos()) {
        typoSnapshot.increment(event.getLanguage(), typo.getExpected());

        String field =
            TodayWordTypoDetailSnapshot.toKey(
                event.getLanguage(), typo.getExpected(), typo.getActual());
        TodayWordTypoDetailEntry entry = getTypoDetailEntry(detailKey, field);
        entry.increment(typo.getType());
        setTypoDetailEntry(detailKey, field, entry);
      }
    }

    setTypoSnapshot(typoKey, typoSnapshot);
    ensureTtl(detailKey);
  }

  public TodayWordTypingSnapshot getTyping(Long memberId, WordLanguage language) {
    String key = typingKey(memberId, language);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      return deserialize(json, TodayWordTypingSnapshot.class);
    }

    return fallbackTyping(memberId, language);
  }

  public TodayWordTypoSnapshot getTypo(Long memberId) {
    String key = typoKey(memberId);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      return deserialize(json, TodayWordTypoSnapshot.class);
    }

    return fallbackTypo(memberId);
  }

  public TodayWordTypoSnapshot getTypoByLanguage(Long memberId, WordLanguage language) {
    TodayWordTypoSnapshot snapshot = getTypo(memberId);
    String prefix = language + ":";
    Map<String, Integer> filtered = new HashMap<>();

    for (Map.Entry<String, Integer> entry : snapshot.getTypoCountMap().entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        filtered.put(entry.getKey(), entry.getValue());
      }
    }

    return TodayWordTypoSnapshot.create(filtered);
  }

  public TodayWordTypoDetailSnapshot getTypoDetail(Long memberId) {
    String key = typoDetailKey(memberId);
    Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

    if (!entries.isEmpty()) {
      Map<String, TodayWordTypoDetailEntry> map = new HashMap<>();
      for (Map.Entry<Object, Object> e : entries.entrySet()) {
        map.put(
            (String) e.getKey(),
            deserialize((String) e.getValue(), TodayWordTypoDetailEntry.class));
      }
      return TodayWordTypoDetailSnapshot.create(map);
    }

    return fallbackTypoDetail(memberId);
  }

  public TodayWordTypoDetailSnapshot getTypoDetailByLanguageAndExpected(
      Long memberId, WordLanguage language, String expected) {
    TodayWordTypoDetailSnapshot snapshot = getTypoDetail(memberId);
    String prefix = language + ":" + expected + ":";

    Map<String, TodayWordTypoDetailEntry> filtered = new HashMap<>();

    for (Map.Entry<String, TodayWordTypoDetailEntry> entry : snapshot.getDetailMap().entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        filtered.put(entry.getKey(), entry.getValue());
      }
    }

    return TodayWordTypoDetailSnapshot.create(filtered);
  }

  private TodayWordTypingSnapshot getSnapshotOrEmpty(String key) {
    String json = redisTemplate.opsForValue().get(key);
    if (json != null) {
      return deserialize(json, TodayWordTypingSnapshot.class);
    }
    return TodayWordTypingSnapshot.empty();
  }

  private TodayWordTypoSnapshot getTypoSnapshotOrEmpty(String key) {
    String json = redisTemplate.opsForValue().get(key);
    if (json != null) {
      return deserialize(json, TodayWordTypoSnapshot.class);
    }
    return TodayWordTypoSnapshot.empty();
  }

  private TodayWordTypoDetailEntry getTypoDetailEntry(String hashKey, String field) {
    Object value = redisTemplate.opsForHash().get(hashKey, field);
    if (value != null) {
      return deserialize((String) value, TodayWordTypoDetailEntry.class);
    }
    return TodayWordTypoDetailEntry.empty();
  }

  private void setSnapshot(String key, TodayWordTypingSnapshot snapshot) {
    String json = serialize(snapshot);
    redisTemplate.opsForValue().set(key, json, ttlUntilMidnightKst());
  }

  private void setTypoSnapshot(String key, TodayWordTypoSnapshot snapshot) {
    String json = serialize(snapshot);
    redisTemplate.opsForValue().set(key, json, ttlUntilMidnightKst());
  }

  private void setTypoDetailEntry(String hashKey, String field, TodayWordTypoDetailEntry entry) {
    redisTemplate.opsForHash().put(hashKey, field, serialize(entry));
  }

  private void ensureTtl(String key) {
    Long ttl = redisTemplate.getExpire(key);
    if (ttl == null || ttl < 0) {
      redisTemplate.expire(key, ttlUntilMidnightKst());
    }
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

  private TodayWordTypoSnapshot fallbackTypo(Long memberId) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(todayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(todayKst);

    List<MemberWordTypoAggregation> results =
        typoAggregationRepository.aggregateByMemberIdsBetween(List.of(memberId), from, to);

    if (results.isEmpty()) {
      return TodayWordTypoSnapshot.empty();
    }

    Map<String, Integer> typoMap = new HashMap<>();
    for (MemberWordTypoAggregation agg : results) {
      String key = TodayWordTypoSnapshot.toKey(agg.getLanguage(), agg.getExpected());
      typoMap.merge(key, agg.getCount(), Integer::sum);
    }

    TodayWordTypoSnapshot snapshot = TodayWordTypoSnapshot.create(typoMap);
    setTypoSnapshot(typoKey(memberId), snapshot);
    return snapshot;
  }

  private TodayWordTypoDetailSnapshot fallbackTypoDetail(Long memberId) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(todayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(todayKst);

    List<MemberWordTypoAggregation> results =
        typoAggregationRepository.aggregateByMemberIdsBetween(List.of(memberId), from, to);

    if (results.isEmpty()) {
      return TodayWordTypoDetailSnapshot.empty();
    }

    String hashKey = typoDetailKey(memberId);
    Map<String, TodayWordTypoDetailEntry> map = new HashMap<>();

    for (MemberWordTypoAggregation agg : results) {
      String field =
          TodayWordTypoDetailSnapshot.toKey(agg.getLanguage(), agg.getExpected(), agg.getActual());
      TodayWordTypoDetailEntry entry =
          TodayWordTypoDetailEntry.create(
              agg.getCount(),
              agg.getInitialCount(),
              agg.getMedialCount(),
              agg.getFinalCount(),
              agg.getLetterCount());
      setTypoDetailEntry(hashKey, field, entry);
      map.put(field, entry);
    }

    ensureTtl(hashKey);
    return TodayWordTypoDetailSnapshot.create(map);
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
