package com.typingpractice.typing_practice_be.typingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberDailyStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberDailyStatsRepository {
  private final EntityManager em;

  public void save(MemberDailyStats stats) {
    em.persist(stats);
  }

  public Optional<MemberDailyStats> findByMemberIdAndDate(Long memberId, LocalDate date) {
    return em.createQuery(
            "select s from MemberDailyStats s where s.member.id = :memberId and s.date = :date",
            MemberDailyStats.class)
        .setParameter("memberId", memberId)
        .setParameter("date", date)
        .getResultStream()
        .findFirst();
  }

  public List<MemberDailyStats> findByMemberIdAndDateBetween(
      Long memberId, LocalDate from, LocalDate to) {
    return em.createQuery(
            "select s from MemberDailyStats s where s.member.id = :memberId "
                + "and s.date between :from and :to "
                + "order by s.date asc",
            MemberDailyStats.class)
        .setParameter("memberId", memberId)
        .setParameter("from", from)
        .setParameter("to", to)
        .getResultList();
  }
}
