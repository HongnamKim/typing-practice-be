package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberDailyWordStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberDailyWordStatsRepository {
  private final EntityManager em;

  public void save(MemberDailyWordStats stats) {
    em.persist(stats);
  }

  /*
   * 최근 n 개 일별 데이터 조회
   */
  public List<MemberDailyWordStats> findRecentByMemberIdAndLanguage(
      Long memberId, WordLanguage language, int limit) {
    return em.createQuery(
            "select s from MemberDailyWordStats s "
                + "where s.member.id = :memberId "
                + "and s.language = :language "
                + "order by s.date desc",
            MemberDailyWordStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .setMaxResults(limit)
        .getResultList()
        .reversed();
  }

  public Optional<MemberDailyWordStats> findByMemberIdAndDateAndLanguage(
      Long memberId, LocalDate date, WordLanguage language) {
    return em.createQuery(
            "select s from MemberDailyWordStats s "
                + "where s.member.id = :memberId "
                + "and s.date = :date "
                + "and s.language = :language",
            MemberDailyWordStats.class)
        .setParameter("memberId", memberId)
        .setParameter("date", date)
        .setParameter("language", language)
        .getResultStream()
        .findFirst();
  }
}
