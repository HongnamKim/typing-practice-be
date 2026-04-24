package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.WordTypingRecordRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypingAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypingStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.MemberWordTypingAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypingStatsRepository;
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
public class MemberWordTypingStatsBatchService {
  private final MemberRepository memberRepository;
  private final WordTypingRecordRepository wordTypingRecordRepository; // 원본 로그 - mongo
  private final MemberWordTypingAggregationRepository aggregationRepository; // mongo aggregation
  private final MemberWordTypingStatsRepository statsRepository; // 회원 단어 타이핑 누적 통계

  private static final int CHUNK_SIZE = 100;

  @Transactional
  public void runScheduledBatch() {
    LocalDate yesterdayKst = LocalDate.now(TimeUtils.KST).minusDays(1);
    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayKst);

    log.info("MemberWordTypingStats 증분 배치 시작 - 범위: {} ~ {}", from, to);

    List<Long> memberIds = wordTypingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to, false);

    log.info("MemberWordTypingStats 증분 배치 완료 - {}건 처리", totalProcessed);
  }

  @Transactional
  public void runManualRecalculation() {
    log.info("MemberWordTypingStats 전체 재계산 시작");

    List<Long> memberIds = wordTypingRecordRepository.findDistinctMemberIds();
    int totalProcessed = processChunks(memberIds, null, null, true);

    log.info("MemberWordTypingStats 전체 재계산 완료 - {}건 처리", totalProcessed);
  }

  private int processChunks(
      List<Long> memberIds, LocalDateTime from, LocalDateTime to, boolean overwrite) {
    int totalProcessed = 0;

    for (int i = 0; i < memberIds.size(); i += CHUNK_SIZE) {
      List<Long> chunk = memberIds.subList(i, Math.min(i + CHUNK_SIZE, memberIds.size()));

      Map<Long, Member> memberMap =
          memberRepository.findAllByIds(chunk).stream()
              .collect(Collectors.toMap(Member::getId, m -> m));

      List<MemberWordTypingAggregation> aggregations =
          overwrite
              ? aggregationRepository.aggregateByMemberIds(chunk)
              : aggregationRepository.aggregateByMemberIdsBetween(chunk, from, to);

      for (MemberWordTypingAggregation agg : aggregations) {
        Member member = memberMap.get(agg.getMemberId());
        if (member == null) {
          log.warn("Member 미존재, 스킵 - memberId: {}", agg.getMemberId());
          continue;
        }

        upsert(agg, overwrite, member);
        totalProcessed++;
      }
    }

    return totalProcessed;
  }

  private void upsert(MemberWordTypingAggregation agg, boolean overwrite, Member member) {
    MemberWordTypingStats stats =
        statsRepository
            .findByMemberIdAndLanguage(agg.getMemberId(), agg.getLanguage())
            .orElse(null);

    if (stats == null) {
      stats =
          MemberWordTypingStats.create(
              member,
              agg.getLanguage(),
              agg.getTotalAttempts(),
              agg.getTotalWordsAttempted(),
              agg.getAvgWpm(),
              agg.getAvgAcc(),
              agg.getBestWpm(),
              agg.getTotalPracticeTimeMin(),
              agg.getLastPracticedAt());

      statsRepository.save(stats);
    } else if (overwrite) {
      stats.overwrite(
          agg.getTotalAttempts(),
          agg.getTotalWordsAttempted(),
          agg.getAvgWpm(),
          agg.getAvgAcc(),
          agg.getBestWpm(),
          agg.getTotalPracticeTimeMin(),
          agg.getLastPracticedAt());
    } else {
      stats.merge(
          agg.getTotalAttempts(),
          agg.getTotalWordsAttempted(),
          agg.getAvgWpm(),
          agg.getAvgAcc(),
          agg.getBestWpm(),
          agg.getTotalPracticeTimeMin(),
          agg.getLastPracticedAt());
    }
  }
}
