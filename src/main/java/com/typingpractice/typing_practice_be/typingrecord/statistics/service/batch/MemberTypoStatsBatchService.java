package com.typingpractice.typing_practice_be.typingrecord.statistics.service.batch;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.repository.MemberTypoAggregationRepository;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoDetailStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoDetailStatsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypoStatsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberTypoStatsBatchService {
  private final TypingRecordRepository typingRecordRepository;
  private final MemberTypoAggregationRepository memberTypoAggregationRepository;
  private final MemberTypoStatsRepository memberTypoStatsRepository;
  private final MemberTypoDetailStatsRepository memberTypoDetailStatsRepository;
  private final MemberRepository memberRepository;

  private static final int CHUNK_SIZE = 500;

  // 스케줄 배치: 전날 하루치 증분
  @Transactional
  public void runScheduledBatch() {
    LocalDate yesterdayKst = LocalDate.now(TimeUtils.KST).minusDays(1);

    LocalDateTime from = TimeUtils.startOfDayKstToUtc(yesterdayKst);
    LocalDateTime to = TimeUtils.endOfDayKstToUtc(yesterdayKst);

    log.info("MemberTypoStats 증분 배치 시작 - 범위: {} ~ {}", from, to);

    List<Long> memberIds = typingRecordRepository.findDistinctMemberIdsBetween(from, to);
    int totalProcessed = processChunks(memberIds, from, to, false);

    log.info("MemberTypoStats 증분 배치 완료 - {}건 처리", totalProcessed);
  }

  // 어드민: 전체 재계산
  @Transactional
  public void runManualRecalculation() {
    log.info("MemberTypoStats 전체 재계산 시작");

    memberTypoDetailStatsRepository.deleteAllInBatch();
    memberTypoStatsRepository.deleteAllInBatch();

    List<Long> memberIds = typingRecordRepository.findDistinctMemberIds();
    int totalProcessed = processChunks(memberIds, null, null, true);

    log.info("MemberTypoStats 전체 재계산 완료 - {}건 처리", totalProcessed);
  }

  private int processChunks(
      List<Long> memberIds, LocalDateTime from, LocalDateTime to, boolean isManual) {
    int totalProcessed = 0;

    for (int i = 0; i < memberIds.size(); i += CHUNK_SIZE) {
      List<Long> chunk = memberIds.subList(i, Math.min(i + CHUNK_SIZE, memberIds.size()));

      Map<Long, Member> memberMap =
          memberRepository.findAllByIds(chunk).stream()
              .collect(Collectors.toMap(Member::getId, m -> m));

      List<MemberTypoAggregation> aggregations =
          isManual
              ? memberTypoAggregationRepository.aggregateByMemberIds(chunk)
              : memberTypoAggregationRepository.aggregateByMemberIdsBetween(chunk, from, to);

      for (MemberTypoAggregation agg : aggregations) {
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
                MemberTypoAggregation first = group.getFirst();
                Member member = memberMap.get(first.getMemberId());
                if (member == null) return;

                int totalCount = group.stream().mapToInt(MemberTypoAggregation::getCount).sum();
                upsertTypoStats(
                    member, first.getLanguage(), first.getExpected(), totalCount, isManual);
              });
    }

    return totalProcessed;
  }

  private void upsertDetail(MemberTypoAggregation agg, boolean isManual, Member member) {
    // 기존 정보 merge
    if (!isManual) {
      MemberTypoDetailStats stats =
          memberTypoDetailStatsRepository
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

    // 새로 생성
    memberTypoDetailStatsRepository.save(
        MemberTypoDetailStats.create(
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
      Member member, QuoteLanguage language, String expected, int count, boolean isManual) {
    if (!isManual) {
      MemberTypoStats stats =
          memberTypoStatsRepository
              .findByMemberIdAndLanguageAndExpected(member.getId(), language, expected)
              .orElse(null);

      if (stats != null) {
        stats.merge(count);
        return;
      }
    }

    memberTypoStatsRepository.save(MemberTypoStats.create(member, language, expected, count));
  }
}
