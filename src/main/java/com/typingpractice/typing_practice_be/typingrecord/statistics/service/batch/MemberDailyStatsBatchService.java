package com.typingpractice.typing_practice_be.typingrecord.statistics.service.batch;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberDailyAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberDailyStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberDailyAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberDailyStatsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberDailyStatsBatchService {
  private final TypingRecordRepository typingRecordRepository;
  private final MemberDailyAggregationRepository memberDailyAggregationRepository;
  private final MemberDailyStatsRepository memberDailyStatsRepository;
  private final MemberRepository memberRepository;

  private static final int CHUNK_SIZE = 500;

  @Transactional
  public void runScheduledBatch() {
    LocalDate yesterdayKst = LocalDate.now(TimeUtils.KST).minusDays(1);

    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayKst);

    log.info("MemberDailyStats 배치 시작 - 범위: {} ~ {}", from, to);

    List<Long> memberIds = typingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to);

    log.info("MemberDailyStats 배치 완료 - {}건 처리", totalProcessed);
  }

  @Transactional
  public void runRecalculationForDate(LocalDate date) {
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(date);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(date);

    log.info("MemberDailyStats 날짜 재계산 시작 - 날짜: {} (범위: {} ~ {})", date, from, to);

    List<Long> memberIds = typingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to);

    log.info("MemberDailyStats 날짜 재계산 완료 - {}건 처리", totalProcessed);
  }

  private int processChunks(List<Long> memberIds, LocalDateTime from, LocalDateTime to) {
    int totalProcessed = 0;

    for (int i = 0; i < memberIds.size(); i += CHUNK_SIZE) {
      List<Long> chunk = memberIds.subList(i, Math.min(i + CHUNK_SIZE, memberIds.size()));

      List<MemberDailyAggregation> aggregations =
          memberDailyAggregationRepository.aggregateByMemberIdsBetween(chunk, from, to);

      for (MemberDailyAggregation agg : aggregations) {
        upsert(agg);
        totalProcessed++;
      }
    }

    return totalProcessed;
  }

  private void upsert(MemberDailyAggregation agg) {
    LocalDate date = agg.getDateAsLocalDate();
    MemberDailyStats stats =
        memberDailyStatsRepository
            .findByMemberIdAndDateAndLanguage(agg.getMemberId(), date, agg.getLanguage())
            .orElse(null);

    if (stats == null) {
      Member member = memberRepository.findById(agg.getMemberId()).orElse(null);
      if (member == null) {
        log.warn("Member 미존재, 스킵 - memberId: {}", agg.getMemberId());
        return;
      }
      stats =
          MemberDailyStats.create(
              member,
              date,
              agg.getLanguage(),
              agg.getAttempts(),
              agg.getAvgCpm(),
              agg.getAvgAcc(),
              agg.getBestCpm(),
              agg.getResetCount(),
              agg.getPracticeTimeMin());
      memberDailyStatsRepository.save(stats);
    } else {
      stats.overwrite(
          agg.getAttempts(),
          agg.getAvgCpm(),
          agg.getAvgAcc(),
          agg.getBestCpm(),
          agg.getResetCount(),
          agg.getPracticeTimeMin());
    }
  }
}
