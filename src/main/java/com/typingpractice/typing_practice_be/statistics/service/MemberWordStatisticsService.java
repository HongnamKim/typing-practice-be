package com.typingpractice.typing_practice_be.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.statistics.exception.RefreshCooldownException;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.response.MemberDailyWordStatsResponse;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.response.MemberWordTypingStatsResponse;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.response.MemberWordTypoDetailStatsResponse;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.response.MemberWordTypoStatsResponse;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.WordTypingRecordRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberDailyWordAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypingAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypoAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberDailyWordStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypingStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoDetailStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoDetailSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypoSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberDailyWordAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypingAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.TodayWordTypingSnapshot;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberDailyWordStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypingStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypoDetailStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypoStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service.TodayWordTypingStatsRedisService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberWordStatisticsService {
  // mongo
  private final WordTypingRecordRepository wordTypingRecordRepository;
  private final MemberWordTypingAggregationRepository memberWordTypingAggregationRepository;
  private final MemberDailyWordAggregationRepository memberDailyWordAggregationRepository;
  private final MemberWordTypoAggregationRepository memberWordTypoAggregationRepository;

  // pg
  private final MemberWordTypingStatsRepository memberWordTypingStatsRepository;
  private final MemberDailyWordStatsRepository memberDailyWordStatsRepository;
  private final MemberWordTypoStatsRepository memberWordTypoStatsRepository;
  private final MemberWordTypoDetailStatsRepository memberWordTypoDetailStatsRepository;

  // redis
  private final TodayWordTypingStatsRedisService todayWordTypingStatsRedisService;

  // refresh
  private final StringRedisTemplate redisTemplate;
  private static final String COOLDOWN_KEY_PREFIX = "cooldown:word-refresh:";
  private static final Duration COOLDOWN_DURATION = Duration.ofMinutes(1);

  private boolean isYesterdayBatchPending(Long memberId, WordLanguage language) {
    LocalDate yesterday = LocalDate.now(TimeUtils.KST).minusDays(1);

    // DailyStats 가 있으면 배치 처리 완료
    if (memberDailyWordStatsRepository
        .findByMemberIdAndDateAndLanguage(memberId, yesterday, language)
        .isPresent()) {
      return false;
    }

    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterday);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterday);
    // 어제 타이핑 기록이 있으면 배치 미완료 없으면 대상 아님
    return wordTypingRecordRepository.existsByMemberIdBetween(memberId, from, to);
  }

  public MemberWordTypingStatsResponse getTypingStats(Long memberId, WordLanguage language) {
    MemberWordTypingStats pg =
        memberWordTypingStatsRepository.findByMemberIdAndLanguage(memberId, language).orElse(null);

    TodayWordTypingSnapshot today = todayWordTypingStatsRedisService.getTyping(memberId, language);

    MemberWordTypingAggregation yesterday = null;
    if (isYesterdayBatchPending(memberId, language)) {
      LocalDate yesterdayDate = LocalDate.now(TimeUtils.KST).minusDays(1);
      LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayDate);
      LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayDate);

      List<MemberWordTypingAggregation> agg =
          memberWordTypingAggregationRepository.aggregateByMemberIdsAndLanguageBetween(
              List.of(memberId), language, from, to);

      if (!agg.isEmpty()) {
        yesterday = agg.getFirst();
      }
    }

    return MemberWordTypingStatsResponse.of(pg, yesterday, today);
  }

  public MemberDailyWordStatsResponse getDailyStats(
      Long memberId, WordLanguage language, int days) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);

    List<MemberDailyWordStats> pgList =
        memberDailyWordStatsRepository.findRecentByMemberIdAndLanguage(memberId, language, days);

    TodayWordTypingSnapshot today = todayWordTypingStatsRedisService.getTyping(memberId, language);

    MemberDailyWordAggregation yesterdayAgg = null;
    if (isYesterdayBatchPending(memberId, language)) {
      LocalDate yesterday = todayKst.minusDays(1);
      LocalDateTime yesterdayFrom = TimeUtils.startOfDayKstToUtc(yesterday);
      LocalDateTime yesterdayTo = TimeUtils.endOfDayKstToUtc(yesterday);

      List<MemberDailyWordAggregation> agg =
          memberDailyWordAggregationRepository.aggregateByMemberIdsAndLanguageBetween(
              List.of(memberId), language, yesterdayFrom, yesterdayTo);

      if (!agg.isEmpty()) {
        yesterdayAgg = agg.getFirst();
      }
    }

    return MemberDailyWordStatsResponse.of(days, pgList, yesterdayAgg, today, todayKst);
  }

  public MemberWordTypoStatsResponse getTypoStats(Long memberId, WordLanguage language) {
    List<MemberWordTypoStats> pgList =
        memberWordTypoStatsRepository.findTop10ByMemberIdAndLanguage(memberId, language);

    TodayWordTypoSnapshot today =
        todayWordTypingStatsRedisService.getTypoByLanguage(memberId, language);

    List<MemberWordTypoAggregation> yesterdayList = List.of();
    if (isYesterdayBatchPending(memberId, language)) {
      LocalDate yesterday = LocalDate.now(TimeUtils.KST).minusDays(1);
      LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterday);
      LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterday);

      yesterdayList =
          memberWordTypoAggregationRepository.aggregateByMemberIdsBetween(
              List.of(memberId), from, to);
    }

    return MemberWordTypoStatsResponse.of(language, pgList, yesterdayList, today);
  }

  public MemberWordTypoDetailStatsResponse getTypoDetailStats(
      Long memberId, WordLanguage language, String expected) {
    List<MemberWordTypoDetailStats> pgList =
        memberWordTypoDetailStatsRepository.findByMemberIdAndLanguageAndExpected(
            memberId, language, expected);

    TodayWordTypoDetailSnapshot todayFiltered =
        todayWordTypingStatsRedisService.getTypoDetailByLanguageAndExpected(
            memberId, language, expected);

    List<MemberWordTypoAggregation> yesterdayList = List.of();
    if (isYesterdayBatchPending(memberId, language)) {
      LocalDate yesterday = LocalDate.now(TimeUtils.KST).minusDays(1);
      LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterday);
      LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterday);

      yesterdayList =
          memberWordTypoAggregationRepository
              .aggregateByMemberIdsBetween(List.of(memberId), from, to)
              .stream()
              .filter(agg -> agg.getExpected().equals(expected))
              .toList();
    }

    return MemberWordTypoDetailStatsResponse.of(
        language, expected, pgList, yesterdayList, todayFiltered);
  }

  public MemberWordTypingStatsResponse refreshStats(Long memberId, WordLanguage language) {
    checkCooldown(memberId);

    todayWordTypingStatsRedisService.invalidateAll(memberId);

    setCooldown(memberId);

    return getTypingStats(memberId, language);
  }

  private void checkCooldown(Long memberId) {
    String key = COOLDOWN_KEY_PREFIX + memberId;
    if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
      throw new RefreshCooldownException();
    }
  }

  private void setCooldown(Long memberId) {
    String key = COOLDOWN_KEY_PREFIX + memberId;
    redisTemplate.opsForValue().set(key, "1", COOLDOWN_DURATION);
  }
}
