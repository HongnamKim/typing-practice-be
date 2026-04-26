package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.service;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.WordTypingRecordRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation.MemberWordTypoAggregationRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoDetailStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypoDetailStatsRepository;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypoStatsRepository;
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
public class MemberWordTypoStatsBatchService {
  private final WordTypingRecordRepository wordTypingRecordRepository;
  private final MemberWordTypoAggregationRepository memberWordTypoAggregationRepository;
  private final MemberWordTypoStatsRepository memberWordTypoStatsRepository;
  private final MemberWordTypoDetailStatsRepository memberWordTypoDetailStatsRepository;
  private final MemberRepository memberRepository;

  private static final int CHUNK_SIZE = 500;

  @Transactional
  public void runScheduledBatch() {
    LocalDate yesterdayKst = LocalDate.now(TimeUtils.KST).minusDays(1);

    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayKst);

    log.info("MemberWordTypoStats 증분 배치 시작 - 범위: {} ~ {}", from, to);

    List<Long> memberIds = wordTypingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to, false);

    log.info("MemberWordTypoStats 증분 배치 완료 - {}건 처리", totalProcessed);
  }

  @Transactional
  public void runManualRecalculation() {
    log.info("MemberWordTypoStats 전체 재계산 시작");

    memberWordTypoDetailStatsRepository.deleteAllInBatch();
    memberWordTypoStatsRepository.deleteAllInBatch();

    List<Long> memberIds = wordTypingRecordRepository.findDistinctMemberIds();
    int totalProcessed = processChunks(memberIds, null, null, true);

    log.info("MemberWordTypoStats 전체 재계산 완료 - {}건 처리", totalProcessed);
  }

  private int processChunks(
      List<Long> memberIds, LocalDateTime from, LocalDateTime to, boolean isManual) {
    int totalProcessed = 0;
    for (int i = 0; i < memberIds.size(); i += CHUNK_SIZE) {
      List<Long> chunk = memberIds.subList(i, Math.min(i + CHUNK_SIZE, memberIds.size()));

      Map<Long, Member> memberMap =
          memberRepository.findAllByIds(chunk).stream()
              .collect(Collectors.toMap(Member::getId, m -> m));

      List<MemberWordTypoAggregation> aggregations =
          isManual
              ? memberWordTypoAggregationRepository.aggregateByMemberIds(chunk)
              : memberWordTypoAggregationRepository.aggregateByMemberIdsBetween(chunk, from, to);

      for (MemberWordTypoAggregation agg : aggregations) {
        Member member = memberMap.get(agg.getMemberId());

        if (member == null) {
          log.warn("Member 미존재, 스킵 - memberId: {}", agg.getMemberId());
          continue;
        }
        upsertDetail(agg, isManual, member);
        totalProcessed++;
      }

      aggregations.stream()
          .collect(
              Collectors.groupingBy(
                  agg -> agg.getMemberId() + "_" + agg.getLanguage() + "_" + agg.getExpected()))
          .forEach(
              (key, group) -> {
                MemberWordTypoAggregation first = group.getFirst();
                Member member = memberMap.get(first.getMemberId());
                if (member == null) return;

                int totalCount = group.stream().mapToInt(MemberWordTypoAggregation::getCount).sum();
                upsertTypoStats(
                    member, first.getLanguage(), first.getExpected(), totalCount, isManual);
              });
    }

    return totalProcessed;
  }

  private void upsertDetail(MemberWordTypoAggregation agg, boolean isManual, Member member) {
    if (!isManual) {
      MemberWordTypoDetailStats stats =
          memberWordTypoDetailStatsRepository
              .findByMemberIdAndLanguageAndExpectedAndActual(
                  member.getId(), agg.getLanguage(), agg.getExpected(), agg.getActual())
              .orElse(null);

      if (stats != null) {
        stats.merge(
            agg.getCount(),
            agg.getInitialCount(),
            agg.getMedialCount(),
            agg.getFinalCount(),
            agg.getLetterCount());
        return;
      }
    }

    memberWordTypoDetailStatsRepository.save(
        MemberWordTypoDetailStats.create(
            member,
            agg.getLanguage(),
            agg.getExpected(),
            agg.getActual(),
            agg.getCount(),
            agg.getInitialCount(),
            agg.getMedialCount(),
            agg.getFinalCount(),
            agg.getLetterCount()));
  }

  private void upsertTypoStats(
      Member member, WordLanguage language, String expected, int count, boolean isManual) {
    if (!isManual) {
      MemberWordTypoStats stats =
          memberWordTypoStatsRepository
              .findByMemberIdAndLanguageAndExpected(member.getId(), language, expected)
              .orElse(null);

      if (stats != null) {
        stats.merge(count);
        return;
      }
    }

    memberWordTypoStatsRepository.save(
        MemberWordTypoStats.create(member, language, expected, count));
  }
}
