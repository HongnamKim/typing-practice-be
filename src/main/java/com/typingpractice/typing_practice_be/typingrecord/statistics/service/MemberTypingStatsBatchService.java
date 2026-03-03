package com.typingpractice.typing_practice_be.typingrecord.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypingAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypingAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
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
public class MemberTypingStatsBatchService {
  private final MemberRepository memberRepository;
  private final TypingRecordRepository typingRecordRepository;
  private final MemberTypingAggregationRepository typingAggregationRepository;
  private final MemberTypingStatsRepository memberTypingStatsRepository;

  private static final int CHUNK_SIZE = 500;

  // 스케줄 배치: 전날 하루치 증분
  @Transactional
  public void runScheduledBatch() {
    LocalDate yesterdayKst = LocalDate.now(TimeUtils.KST).minusDays(1);

    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayKst);

    log.info("MemberTypingStats 증분 배치 시작 - 범위: {} ~ {}", from, to);

    List<Long> memberIds = typingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to, false);

    log.info("MemberTypingStats 증분 배치 완료 - {}건 처리", totalProcessed);
  }

  // 어드민: 전체 재계산
  @Transactional
  public void runManualRecalculation() {
    log.info("MemberTypingStats 전체 재계산 시작");

    List<Long> memberIds = typingRecordRepository.findDistinctMemberIds();
    int totalProcessed = processChunks(memberIds, null, null, true);

    log.info("MemberTypingStats 전체 재계산 완료 - {}건 처리", totalProcessed);
  }

  // 어드민: 날짜 범위 재실행
  @Transactional
  public void runRecalculationForPeriod(LocalDateTime from, LocalDateTime to) {
    log.info("MemberTypingStats 기간 재계산 시작 - 범위: {} - {}", from, to);

    List<Long> memberIds = typingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to, false);

    log.info("MemberTypingStats 기간 재계산 완료 - {}건 처리", totalProcessed);
  }

  private int processChunks(
      List<Long> memberIds, LocalDateTime from, LocalDateTime to, boolean overwrite) {
    int totalProcessed = 0;

    for (int i = 0; i < memberIds.size(); i += CHUNK_SIZE) {
      List<Long> chunk = memberIds.subList(i, Math.min(i + CHUNK_SIZE, memberIds.size()));

      List<MemberTypingAggregation> aggregations =
          overwrite
              ? typingAggregationRepository.aggregateByMemberIds(chunk)
              : typingAggregationRepository.aggregateByMemberIdsBetween(chunk, from, to);

      for (MemberTypingAggregation agg : aggregations) {
        upsert(agg.getMemberId(), agg, overwrite);
        totalProcessed++;
      }
    }

    return totalProcessed;
  }

  private void upsert(Long memberId, MemberTypingAggregation agg, boolean overwrite) {
    MemberTypingStats stats = memberTypingStatsRepository.findByMemberId(memberId).orElse(null);

    if (stats == null) {
      Member member = memberRepository.findById(memberId).orElse(null);
      if (member == null) {
        log.warn("Member 미존재, 스킵 - memberId: {}", memberId);
        return;
      }

      stats =
          MemberTypingStats.create(
              member,
              agg.getTotalAttempts(),
              (float) agg.getAvgCpm(),
              (float) agg.getAvgAcc(),
              agg.getBestCpm(),
              agg.getTotalPracticeTimeMin(),
              agg.getTotalResetCount(),
              agg.getLastPracticedAt());
      memberTypingStatsRepository.save(stats);
    } else if (overwrite) {
      stats.overwrite(
          agg.getTotalAttempts(),
          (float) agg.getAvgCpm(),
          (float) agg.getAvgAcc(),
          agg.getBestCpm(),
          agg.getTotalPracticeTimeMin(),
          agg.getTotalResetCount(),
          agg.getLastPracticedAt());
    } else {
      stats.merge(
          agg.getTotalAttempts(),
          (float) agg.getAvgCpm(),
          (float) agg.getAvgAcc(),
          agg.getBestCpm(),
          agg.getTotalPracticeTimeMin(),
          agg.getTotalResetCount(),
          agg.getLastPracticedAt());
    }
  }
}
