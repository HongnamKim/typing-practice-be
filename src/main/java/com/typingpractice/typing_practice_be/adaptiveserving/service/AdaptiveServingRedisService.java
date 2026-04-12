package com.typingpractice.typing_practice_be.adaptiveserving.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.adaptiveserving.config.AdaptiveServingProperties;
import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveServingEstimation;
import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveServingRecord;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.event.TypingRecordSavedEvent;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveServingRedisService {
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final AdaptiveServingCalculator calculator;
  private final AdaptiveServingProperties properties;
  private final MemberTypingStatsRepository memberTypingStatsRepository;

  private final TypingRecordRepository typingRecordRepository;

  private static final String KEY_PREFIX = "adaptive:";

  private String key(Long memberId, QuoteLanguage language) {
    return KEY_PREFIX + memberId + ":" + language;
  }

  private Duration ttlUntilNextBatch() {
    LocalDateTime nowKst = LocalDateTime.now(TimeUtils.KST);
    LocalDateTime nextBatch = nowKst.toLocalDate().plusDays(1).atTime(LocalTime.of(4, 0));
    return Duration.between(nowKst, nextBatch);
  }

  /*
  Redis -> PostgreSQL -> 기본값 순서로 fallback 하여 조회
   */
  public AdaptiveServingEstimation getEstimation(Long memberId, QuoteLanguage language) {
    // 1. Redis
    String json = redisTemplate.opsForValue().get(key(memberId, language));
    if (json != null) {
      return deserialize(json);
    }

    // 2. PostgreSQL fallback 확정값 + MongoDB 오늘 기록으로 재구성
    MemberTypingStats stats =
        memberTypingStatsRepository.findByMemberIdAndLanguage(memberId, language).orElse(null);

    float mu =
        (stats != null && stats.getEstimatedDifficulty() != 0)
            ? stats.getEstimatedDifficulty()
            : properties.getDefaultMu();
    float sigma =
        (stats != null && stats.getEstimatedUncertainty() != 0)
            ? stats.getEstimatedUncertainty()
            : properties.getDefaultSigma();

    LocalDateTime todayStart = TimeUtils.startOfDayKstToUtc(LocalDate.now(TimeUtils.KST));
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    List<AdaptiveServingRecord> records =
        typingRecordRepository.findForAdaptiveServingBetween(memberId, language, todayStart, now);

    for (AdaptiveServingRecord record : records) {
      float perf =
          calculator.calcPerfNormalized(
              record.getCpm(), record.getAccuracy(),
              record.getAvgCpmSnapshot(), record.getAvgAccSnapshot());
      float x = calculator.calcObservation(record.getQuoteDifficulty(), perf);
      float[] updated = calculator.update(mu, sigma, x);
      mu = updated[0];
      sigma = updated[1];
    }

    // Redis에 캐싱
    AdaptiveServingEstimation estimation = AdaptiveServingEstimation.of(mu, sigma);
    setEstimation(memberId, language, estimation);
    return estimation;
  }

  public void updateEstimation(TypingRecordSavedEvent event) {
    if (event.getMemberId() == null) return;
    if (event.isOutlier()) return;
    if (event.getAvgCpmSnapshot() == 0 || event.getAvgAccSnapshot() == 0) return;

    AdaptiveServingEstimation current = getEstimation(event.getMemberId(), event.getLanguage());

    float perfNormalized =
        calculator.calcPerfNormalized(
            event.getCpm(),
            event.getAccuracy(),
            event.getAvgCpmSnapshot(),
            event.getAvgAccSnapshot());

    System.out.println("perfNormalized = " + perfNormalized);

    float x = calculator.calcObservation(event.getDifficulty(), perfNormalized);
    float[] updated = calculator.update(current.getMu(), current.getSigma(), x);

    AdaptiveServingEstimation newEstimation = AdaptiveServingEstimation.of(updated[0], updated[1]);
    setEstimation(event.getMemberId(), event.getLanguage(), newEstimation);
  }

  public void setEstimation(
      Long memberId, QuoteLanguage language, AdaptiveServingEstimation estimation) {
    String json = serialize(estimation);
    redisTemplate.opsForValue().set(key(memberId, language), json, ttlUntilNextBatch());
  }

  public void deleteEstimation(Long memberId, QuoteLanguage language) {
    redisTemplate.delete(key(memberId, language));
  }

  private String serialize(AdaptiveServingEstimation estimation) {
    try {
      return objectMapper.writeValueAsString(estimation);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("AdaptiveServing Redis 직렬화 실패", e);
    }
  }

  private AdaptiveServingEstimation deserialize(String json) {
    try {
      return objectMapper.readValue(json, AdaptiveServingEstimation.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("AdaptiveServing Redis 역직렬화 실패", e);
    }
  }
}
