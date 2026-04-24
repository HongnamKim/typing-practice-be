package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.WordTypingRecordRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberDailyWordAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberDailyWordStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.MemberDailyWordAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberDailyWordStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberDailyWordStatsBatchService {
  private final WordTypingRecordRepository wordTypingRecordRepository;
  private final MemberDailyWordAggregationRepository aggregationRepository;
  private final MemberDailyWordStatsRepository statsRepository;
  private final MemberRepository memberRepository;

  private static final int CHUNK_SIZE = 500;

  @Transactional
  public void runScheduledBatch() {
    LocalDate yesterdayKst = LocalDate.now(TimeUtils.KST).minusDays(1);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayKst);

    log.info("MemberDailyWordStats 배치 시작 - 범위: {} ~ {}", from, to);

    List<Long> memberIds = wordTypingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to);

    log.info("MemberDailyWordStats 배치 완료 - {}건 처리", totalProcessed);
  }

  @Transactional
  public void runRecalculationForDate(LocalDate date) {
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(date);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(date);

    log.info("MemberDailyWordStats 날짜 재계산 시작 - 날짜: {} (범위: {} ~ {})", date, from, to);

    List<Long> memberIds = wordTypingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to);

    log.info("MemberDailyWordStats 날짜 재계산 완료 - {}건 처리", totalProcessed);
  }

  private int processChunks(List<Long> memberIds, LocalDateTime from, LocalDateTime to) {
    int totalProcessed = 0;
    for (int i = 0; i < memberIds.size(); i += CHUNK_SIZE) {
      List<Long> chunk = memberIds.subList(i, Math.min(i + CHUNK_SIZE, memberIds.size()));

      Map<Long, Member> memberMap =
          memberRepository.findAllByIds(chunk).stream()
              .collect(Collectors.toMap(Member::getId, m -> m));

      List<MemberDailyWordAggregation> aggregations =
          aggregationRepository.aggregateByMemberIdsBetween(chunk, from, to);

      for (MemberDailyWordAggregation agg : aggregations) {
        Member member = memberMap.get(agg.getMemberId());
        if (member == null) {
          log.warn("Member 미존재, 스킵 - memberId: {}", agg.getMemberId());
          continue;
        }

        upsert(agg, member);
        totalProcessed++;
      }
    }

    return totalProcessed;
  }

  private void upsert(MemberDailyWordAggregation agg, Member member) {
    LocalDate date = agg.getDateAsLocalDate();
    MemberDailyWordStats stats =
        statsRepository
            .findByMemberIdAndDateAndLanguage(agg.getMemberId(), date, agg.getLanguage())
            .orElse(null);

    if (stats == null) {
      stats =
          MemberDailyWordStats.create(
              member,
              date,
              agg.getLanguage(),
              agg.getAttempts(),
              agg.getWordsAttempted(),
              agg.getAvgWpm(),
              agg.getAvgAcc(),
              agg.getBestWpm(),
              agg.getPracticeTimeMin());

      statsRepository.save(stats);
    } else {
      stats.overwrite(
          agg.getAttempts(),
          agg.getWordsAttempted(),
          agg.getAvgWpm(),
          agg.getAvgAcc(),
          agg.getBestWpm(),
          agg.getPracticeTimeMin());
    }
  }
}
