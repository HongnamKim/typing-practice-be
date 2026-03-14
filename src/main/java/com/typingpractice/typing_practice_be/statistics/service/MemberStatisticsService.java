package com.typingpractice.typing_practice_be.statistics.service;

import com.typingpractice.typing_practice_be.statistics.exception.RefreshCooldownException;
import com.typingpractice.typing_practice_be.typingrecord.dto.MemberTypingStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypingSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.TodayTypingStatsRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberStatisticsService {
  private final MemberTypingStatsRepository memberTypingStatsRepository;
  private final TodayTypingStatsRedisService todayTypingStatsRedisService;
  private final StringRedisTemplate redisTemplate;

  private static final String COOLDOWN_KEY_PREFIX = "cooldown:refresh:";
  private static final Duration COOLDOWN_DURATION = Duration.ofMinutes(1);

  public MemberTypingStatsResponse getTypingStats(Long memberId) {
    MemberTypingStats pg = memberTypingStatsRepository.findByMemberId(memberId).orElse(null);
    TodayTypingSnapshot today = todayTypingStatsRedisService.getTyping(memberId);

    if (pg == null && today.getTotalAttempts() == 0) {
      return MemberTypingStatsResponse.empty();
    }
    if (pg == null) {
      return MemberTypingStatsResponse.from(today);
    }
    if (today.getTotalAttempts() == 0) {
      return MemberTypingStatsResponse.from(pg);
    }

    return MemberTypingStatsResponse.merge(pg, today);
  }

  public MemberTypingStatsResponse refreshStats(Long memberId) {
    checkCooldown(memberId);

    todayTypingStatsRedisService.invalidateTyping(memberId);
    todayTypingStatsRedisService.invalidateTypo(memberId);
    todayTypingStatsRedisService.invalidateTypoDetail(memberId);

    setCooldown(memberId);

    return getTypingStats(memberId);
  }

  private void checkCooldown(Long memberId) {
    String key = COOLDOWN_KEY_PREFIX + memberId;
    if (redisTemplate.hasKey(key)) {
      throw new RefreshCooldownException();
    }
  }

  private void setCooldown(Long memberId) {
    String key = COOLDOWN_KEY_PREFIX + memberId;
    redisTemplate.opsForValue().set(key, "1", COOLDOWN_DURATION);
  }
}
