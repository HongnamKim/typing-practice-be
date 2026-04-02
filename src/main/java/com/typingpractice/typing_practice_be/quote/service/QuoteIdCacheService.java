package com.typingpractice.typing_practice_be.quote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.quote.domain.QuoteDifficultyTier;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteIdCacheService {
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final QuoteRepository quoteRepository;

  private static final Duration PUBLIC_TTL = Duration.ofMinutes(10);

  private String publicKey(QuoteLanguage language, QuoteDifficultyTier tier) {
    return "quote:public-ids:" + language.name() + ":" + tier.name();
  }

  private String memberKey(Long memberId, QuoteLanguage language) {
    return "quote:member-ids:" + memberId + ":" + language.name();
  }

  public List<Long> getPublicIds(QuoteLanguage language, QuoteDifficultyTier tier) {
    String key = publicKey(language, tier);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      // log.info("[QuoteIdCache] 공개 문장 ID 캐시 히트 - language={}", language);
      return deserialize(json);
    }

    List<Long> ids = quoteRepository.findAllPublicIds(language, tier);
    redisTemplate.opsForValue().set(key, serialize(ids), PUBLIC_TTL);
    log.info("[QuoteIdCache] 공개 문장 ID 캐시 로드 - language={}, size={}", language, ids.size());
    return ids;
  }

  public List<Long> getIdsByMemberId(Long memberId, QuoteLanguage language) {
    String key = memberKey(memberId, language);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      return deserialize(json);
    }

    List<Long> ids = quoteRepository.findIdsByMemberId(memberId, language);
    redisTemplate.opsForValue().set(key, serialize(ids));
    log.info(
        "[QuoteIdCache] 멤버 문장 ID 캐시 로드 - memberId={}, language={}, size={}",
        memberId,
        language,
        ids.size());
    return ids;
  }

  public void refreshAllPublicIds() {
    for (QuoteLanguage language : QuoteLanguage.values()) {
      for (QuoteDifficultyTier tier : QuoteDifficultyTier.values()) {
        List<Long> ids = quoteRepository.findAllPublicIds(language, tier);
        redisTemplate.opsForValue().set(publicKey(language, tier), serialize(ids), PUBLIC_TTL);
        log.info(
            "[QuoteIdCache] 공개 문장 ID 캐시 갱신 - language={}, tier={}, size={}",
            language,
            tier,
            ids.size());
      }
    }
  }

  public void invalidateMemberIds(Long memberId, QuoteLanguage language) {
    redisTemplate.delete(memberKey(memberId, language));
    log.info("[QuoteIdCache] 멤버 문장 ID 캐시 삭제 - memberId={}, language={}", memberId, language);
  }

  private String serialize(List<Long> ids) {
    try {
      return objectMapper.writeValueAsString(ids);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("QuoteIdCache 직렬화 실패", e);
    }
  }

  private List<Long> deserialize(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
    } catch (JsonProcessingException e) {
      throw new RuntimeException("QuoteIdCache 역직렬화 실패", e);
    }
  }
}
