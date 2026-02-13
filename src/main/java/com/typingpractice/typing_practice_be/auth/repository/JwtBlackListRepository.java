package com.typingpractice.typing_practice_be.auth.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JwtBlackListRepository {

  private final StringRedisTemplate redisTemplate;

  private static final String KEY_PREFIX = "blacklist:";

  public void save(String jwtId, LocalDateTime expiresIn) {
    Duration ttl = Duration.between(LocalDateTime.now(ZoneOffset.UTC), expiresIn);

    if (ttl.isNegative() || ttl.isZero()) {
      return;
    }

    redisTemplate.opsForValue().set(KEY_PREFIX + jwtId, "true", ttl);
  }

  public boolean existByJwtId(String jwtId) {
    return redisTemplate.hasKey(KEY_PREFIX + jwtId);
  }
}
