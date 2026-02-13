package com.typingpractice.typing_practice_be.auth.repository;

import com.typingpractice.typing_practice_be.auth.domain.JwtBlackList;
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

  public void save(JwtBlackList jwtBlackList) {
    Duration ttl = Duration.between(LocalDateTime.now(ZoneOffset.UTC), jwtBlackList.getExpiresIn());

    if (ttl.isNegative() || ttl.isZero()) {
      return;
    }

    redisTemplate.opsForValue().set(KEY_PREFIX + jwtBlackList.getJwtId(), "true", ttl);
  }

  public boolean existByJwtId(String jwtId) {
    return redisTemplate.hasKey(KEY_PREFIX + jwtId);
  }
}
