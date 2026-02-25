package com.typingpractice.typing_practice_be.typingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberTypingStatsRepository {
  private final EntityManager em;

  public void save(MemberTypingStats stats) {
    em.persist(stats);
  }

  public Optional<MemberTypingStats> findByMemberId(Long memberId) {
    return em.createQuery(
            "select s from MemberTypingStats s where s.member.id = :memberId",
            MemberTypingStats.class)
        .setParameter("memberId", memberId)
        .getResultStream()
        .findFirst();
  }
}
