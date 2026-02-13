package com.typingpractice.typing_practice_be.auth.repository;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
  private final StringRedisTemplate redisTemplate;

  private static final String TOKEN_PREFIX = "refresh:";
  private static final String MEMBER_PREFIX = "refresh:member:";

  public void save(Long memberId, String token, Duration ttl) {
    redisTemplate.opsForValue().set(TOKEN_PREFIX + token, String.valueOf(memberId), ttl);
    redisTemplate.opsForValue().set(MEMBER_PREFIX + memberId, token, ttl);
  }

  public Optional<Long> findMemberIdByToken(String token) {
    String memberId = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);

    if (memberId == null) {
      return Optional.empty();
    }

    return Optional.of(Long.valueOf(memberId));
  }

  public void deleteByToken(String token) {
    String memberId = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);

    redisTemplate.delete(TOKEN_PREFIX + token);

    if (memberId != null) {
      redisTemplate.delete(MEMBER_PREFIX + memberId);
    }
  }

  public void deleteByMemberId(Long memberId) {
    String token = redisTemplate.opsForValue().get(MEMBER_PREFIX + memberId);
    redisTemplate.delete(MEMBER_PREFIX + memberId);

    if (token != null) {
      redisTemplate.delete(TOKEN_PREFIX + token);
    }
  }
}
