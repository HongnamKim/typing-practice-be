package com.typingpractice.typing_practice_be.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.exception.RefreshCooldownException;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberDailyStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypingStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoDetailStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberDailyAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypingAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypoAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberDailyStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoDetailStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.*;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberDailyStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoDetailStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.TodayTypingStatsRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberStatisticsService {
  private final TypingRecordRepository typingRecordRepository;
  private final MemberTypingAggregationRepository memberTypingAggregationRepository;
  private final MemberDailyAggregationRepository memberDailyAggregationRepository;
  private final MemberTypoAggregationRepository memberTypoAggregationRepository;

  private final MemberTypingStatsRepository memberTypingStatsRepository;
  private final MemberDailyStatsRepository memberDailyStatsRepository;
  private final MemberTypoStatsRepository memberTypoStatsRepository;
  private final MemberTypoDetailStatsRepository memberTypoDetailStatsRepository;

  private final TodayTypingStatsRedisService todayTypingStatsRedisService;
  private final StringRedisTemplate redisTemplate;

  private static final String COOLDOWN_KEY_PREFIX = "cooldown:refresh:";
  private static final Duration COOLDOWN_DURATION = Duration.ofMinutes(1);

  private boolean isYesterdayBatchPending(Long memberId, QuoteLanguage language) {
    LocalDate yesterday = LocalDate.now(TimeUtils.KST).minusDays(1);

    // PostgreSQL 에 어제 DailyStats 있으면 배치 완료
    if (memberDailyStatsRepository
        .findByMemberIdAndDateAndLanguage(memberId, yesterday, language)
        .isPresent()) {
      return false;
    }

    // MongoDB에 어제 기록 있으면 배치 미완료
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterday);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterday);
    return typingRecordRepository.existsByMemberIdBetween(memberId, from, to);
  }

  public MemberTypingStatsResponse getTypingStats(Long memberId, QuoteLanguage language) {
    MemberTypingStats pg =
        memberTypingStatsRepository.findByMemberIdAndLanguage(memberId, language).orElse(null);
    TodayTypingSnapshot today = todayTypingStatsRedisService.getTyping(memberId, language);

    MemberTypingAggregation yesterday = null;
    if (isYesterdayBatchPending(memberId, language)) {
      LocalDate yesterdayDate = LocalDate.now(TimeUtils.KST).minusDays(1);
      LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayDate);
      LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayDate);

      List<MemberTypingAggregation> agg =
          memberTypingAggregationRepository.aggregateByMemberIdsAndLanguageBetween(
              List.of(memberId), language, from, to);

      if (!agg.isEmpty()) {
        yesterday = agg.getFirst();
      }
    }

    //    if (pg == null && today.getTotalAttempts() == 0) {
    //      return MemberTypingStatsResponse.empty();
    //    }
    //    if (pg == null) {
    //      return MemberTypingStatsResponse.from(today);
    //    }
    //    if (today.getTotalAttempts() == 0) {
    //      return MemberTypingStatsResponse.from(pg);
    //    }
    //
    //    return MemberTypingStatsResponse.merge(pg, today);

    return MemberTypingStatsResponse.of(pg, yesterday, today);
  }

  public MemberDailyStatsResponse getDailyStats(Long memberId, QuoteLanguage language, int days) {
    LocalDate todayKst = LocalDate.now(TimeUtils.KST);
    LocalDate from = todayKst.minusDays(days - 1);
    LocalDate yesterday = todayKst.minusDays(1);

    List<MemberDailyStats> pgList =
        memberDailyStatsRepository.findByMemberIdAndLanguageAndDateBetween(
            memberId, language, from, yesterday);

    TodayTypingSnapshot today = todayTypingStatsRedisService.getTyping(memberId, language);

    MemberDailyAggregation yesterdayAgg = null;
    if (isYesterdayBatchPending(memberId, language)) {
      LocalDateTime yesterdayFrom = TimeUtils.startOfDayKstToUtc(yesterday);
      LocalDateTime yesterdayTo = TimeUtils.endOfDayKstToUtc(yesterday);

      List<MemberDailyAggregation> agg =
          memberDailyAggregationRepository.aggregateByMemberIdsAndLanguageBetween(
              List.of(memberId), language, yesterdayFrom, yesterdayTo);

      if (!agg.isEmpty()) {
        yesterdayAgg = agg.getFirst();
      }
    }

    return MemberDailyStatsResponse.of(days, pgList, yesterdayAgg, today, todayKst);
  }

  public MemberTypoStatsResponse getTypoStats(Long memberId, QuoteLanguage language) {
    List<MemberTypoStats> pgList =
        memberTypoStatsRepository.findTop10ByMemberIdAndLanguage(memberId, language);

    TodayTypoSnapshot today = todayTypingStatsRedisService.getTypoByLanguage(memberId, language);

    List<MemberTypoAggregation> yesterdayList = List.of();
    if (isYesterdayBatchPending(memberId, language)) {
      LocalDate yesterday = LocalDate.now(TimeUtils.KST).minusDays(1);
      LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterday);
      LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterday);

      yesterdayList =
          memberTypoAggregationRepository.aggregateByMemberIdsBetween(List.of(memberId), from, to);
    }

    return MemberTypoStatsResponse.of(language, pgList, yesterdayList, today);
    // return MemberTypoStatsResponse.of(language, pgList, today);
  }

  public MemberTypoDetailStatsResponse getTypoDetailStats(
      Long memberId, QuoteLanguage language, String expected) {
    List<MemberTypoDetailStats> pgList =
        memberTypoDetailStatsRepository.findByMemberIdAndLanguageAndExpected(
            memberId, language, expected);

    TodayTypoDetailSnapshot todayFiltered =
        todayTypingStatsRedisService.getTypoDetailByLanguageAndExpected(
            memberId, language, expected);

    List<MemberTypoAggregation> yesterdayList = List.of();
    if (isYesterdayBatchPending(memberId, language)) {
      LocalDate yesterday = LocalDate.now(TimeUtils.KST).minusDays(1);
      LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterday);
      LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterday);

      yesterdayList =
          memberTypoAggregationRepository
              .aggregateByMemberIdsBetween(List.of(memberId), from, to)
              .stream()
              .filter(agg -> agg.getExpected().equals(expected))
              .toList();
    }
    return MemberTypoDetailStatsResponse.of(
        language, expected, pgList, yesterdayList, todayFiltered);
    // return MemberTypoDetailStatsResponse.of(language, expected, pgList, todayFiltered);
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
