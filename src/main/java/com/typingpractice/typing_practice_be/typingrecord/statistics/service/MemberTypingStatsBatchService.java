package com.typingpractice.typing_practice_be.typingrecord.statistics.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypingAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
  private final MemberTypingStatsRepository memberTypingStatsRepository;

  private static final int CHUNK_SIZE = 500;

  @Transactional
  public void runScheduledBatch() {
    LocalDateTime from = LocalDate.now().minusDays(1).atStartOfDay(); // 어제 00시
    LocalDateTime to = LocalDate.now().minusDays(1).atTime(LocalTime.MAX); // 어제 23시 59분

    log.info("MemberTypingStats 증분 배치 시작 - 범위: {} ~ {}", from, to);

    List<Long> memberIds = typingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to);

    log.info("MemberTypingStats 증분 배치 완료 - {}건 처리", totalProcessed);
  }

  @Transactional
  public void refreshForMember(Long memberId) {
    LocalDateTime from = LocalDate.now().atStartOfDay(); // 오늘 00시
    LocalDateTime to = LocalDateTime.now(); // 지금

    List<MemberTypingAggregation> aggregations =
        typingRecordRepository.aggregateByMemberIdsBetween(List.of(memberId), from, to);

    if (aggregations.isEmpty()) return;

    MemberTypingAggregation agg = aggregations.getFirst();
    upsert(memberId, agg);

    log.info("MemberTypingStats 유저 새로고침 완료 - memberId: {}", memberId);
  }

  private int processChunks(List<Long> memberIds, LocalDateTime from, LocalDateTime to) {
    int totalProcessed = 0;

    // CHUNK_SIZE 만큼 잘라서 배치 처리
    for (int i = 0; i < memberIds.size(); i += CHUNK_SIZE) {
      List<Long> chunk = memberIds.subList(i, Math.min(i + CHUNK_SIZE, memberIds.size()));

      List<MemberTypingAggregation> aggregations =
          typingRecordRepository.aggregateByMemberIdsBetween(chunk, from, to);

      for (MemberTypingAggregation agg : aggregations) {
        upsert(agg.getMemberId(), agg);
        totalProcessed++;
      }
    }

    return totalProcessed;
  }

  private void upsert(Long memberId, MemberTypingAggregation agg) {
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
