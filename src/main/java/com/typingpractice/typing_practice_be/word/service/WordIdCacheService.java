package com.typingpractice.typing_practice_be.word.service;

import com.typingpractice.typing_practice_be.word.domain.WordDifficultyTier;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.repository.WordRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordIdCacheService {
  private final StringRedisTemplate redisTemplate;
  private final WordRepository wordRepository;

  private static final String KEY_PREFIX = "word:";

  private String key(WordLanguage language) {
    return KEY_PREFIX + language.name();
  }

  public void buildCache(WordLanguage language) {
    List<Object[]> rows = wordRepository.findAllIdsWithDifficulty(language);
    if (rows.isEmpty()) return;

    String k = key(language);
    redisTemplate.delete(k);

    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
    for (Object[] row : rows) {
      Long id = (Long) row[0];
      float difficulty = row[1] != null ? ((Number) row[1]).floatValue() : 0f;
      tuples.add(ZSetOperations.TypedTuple.of(String.valueOf(id), (double) difficulty));
    }

    redisTemplate.opsForZSet().add(k, tuples);
    log.info("[WordIdCache] 캐시 빌드 - language={}, size={}", language, tuples.size());
  }

  public void buildAllCaches() {
    for (WordLanguage language : WordLanguage.values()) {
      buildCache(language);
    }
  }

  public void add(WordLanguage language, Long wordId, float difficulty) {
    redisTemplate.opsForZSet().add(key(language), String.valueOf(wordId), difficulty);
  }

  public void remove(WordLanguage language, Long wordId) {
    redisTemplate.opsForZSet().remove(key(language), String.valueOf(wordId));
  }

  public List<Long> getIdsByTier(WordLanguage language, WordDifficultyTier tier) {
    String k = key(language);

    try {
      Long totalSize = redisTemplate.opsForZSet().size(k);
      if (totalSize == null || totalSize == 0) {
        return fallback(language, tier);
      }

      Set<String> members;
      if (tier == WordDifficultyTier.RANDOM) {
        members = redisTemplate.opsForZSet().range(k, 0, -1);
      } else {
        long easyEnd = totalSize / 3;
        long normalEnd = totalSize * 2 / 3;

        members =
            switch (tier) {
              case EASY -> redisTemplate.opsForZSet().range(k, 0, easyEnd - 1);
              case NORMAL -> redisTemplate.opsForZSet().range(k, easyEnd, normalEnd - 1);
              case HARD -> redisTemplate.opsForZSet().range(k, normalEnd, -1);
              default -> redisTemplate.opsForZSet().range(k, 0, -1);
            };
      }

      if (members == null || members.isEmpty()) {
        return fallback(language, tier);
      }

      return members.stream().map(Long::parseLong).toList();

    } catch (Exception e) {
      log.warn("[WordIdCache] Redis 조회 실패, DB fallback: {}", e.getMessage());
      return fallback(language, tier);
    }
  }

  private List<Long> fallback(WordLanguage language, WordDifficultyTier tier) {
    try {
      buildCache(language);
    } catch (Exception e) {
      log.warn("[WordIdCache] 캐시 복구 실패: {}", e.getMessage());
    }

    List<Long> allIds = wordRepository.findAllIdsSortedByDifficulty(language);

    if (tier == WordDifficultyTier.RANDOM) return allIds;

    int size = allIds.size();
    int easyEnd = size / 3;
    int normalEnd = size * 2 / 3;

    return switch (tier) {
      case EASY -> allIds.subList(0, easyEnd);
      case NORMAL -> allIds.subList(easyEnd, normalEnd);
      case HARD -> allIds.subList(normalEnd, size);
      default -> allIds;
    };
  }
}
