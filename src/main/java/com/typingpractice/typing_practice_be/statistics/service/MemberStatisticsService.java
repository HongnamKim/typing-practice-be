package com.typingpractice.typing_practice_be.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.exception.RefreshCooldownException;
import com.typingpractice.typing_practice_be.typingrecord.dto.MemberDailyStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.MemberTypingStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.MemberTypoStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberDailyStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypingSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.TodayTypoSnapshot;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberDailyStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.TodayTypingStatsRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberStatisticsService {
  private final MemberTypingStatsRepository memberTypingStatsRepository;
  private final MemberDailyStatsRepository memberDailyStatsRepository;
  private final MemberTypoStatsRepository memberTypoStatsRepository;

  private final TodayTypingStatsRedisService todayTypingStatsRedisService;
  private final StringRedisTemplate redisTemplate;

  private static final String COOLDOWN_KEY_PREFIX = "cooldown:refresh:";
  private static final Duration COOLDOWN_DURATION = Duration.ofMinutes(1);

  public MemberTypingStatsResponse getTypingStats(Long memberId, QuoteLanguage language) {
    MemberTypingStats pg =
        memberTypingStatsRepository.findByMemberIdAndLanguage(memberId, language).orElse(null);
    TodayTypingSnapshot today = todayTypingStatsRedisService.getTyping(memberId, language);

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

  public MemberDailyStatsResponse getDailyStats(Long memberId, QuoteLanguage language, int days) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);
    LocalDate from = todayKst.minusDays(days - 1);
    LocalDate yesterday = todayKst.minusDays(1);

    List<MemberDailyStats> pgList =
        memberDailyStatsRepository.findByMemberIdAndLanguageAndDateBetween(
            memberId, language, from, yesterday);

    TodayTypingSnapshot today = todayTypingStatsRedisService.getTyping(memberId, language);

    return MemberDailyStatsResponse.of(days, pgList, today, todayKst);
  }

  public MemberTypoStatsResponse getTypoStats(Long memberId, QuoteLanguage language) {
    List<MemberTypoStats> pgList =
        memberTypoStatsRepository.findTop10ByMemberIdAndLanguage(memberId, language);

    TodayTypoSnapshot today = todayTypingStatsRedisService.getTypoByLanguage(memberId, language);

    return MemberTypoStatsResponse.of(language, pgList, today);
  }

  public MemberTypingStatsResponse refreshStats(Long memberId, QuoteLanguage language) {
    checkCooldown(memberId);

    for (QuoteLanguage lang : QuoteLanguage.values()) {
      todayTypingStatsRedisService.invalidateTyping(memberId, lang);
    }

    todayTypingStatsRedisService.invalidateTypo(memberId);
    todayTypingStatsRedisService.invalidateTypoDetail(memberId);

    setCooldown(memberId);

    return getTypingStats(memberId, language);
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
