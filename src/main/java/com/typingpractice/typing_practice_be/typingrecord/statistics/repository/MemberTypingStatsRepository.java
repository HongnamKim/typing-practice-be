package com.typingpractice.typing_practice_be.typingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberTypingStatsRepository {
  private final EntityManager em;

  public void save(MemberTypingStats stats) {
    em.persist(stats);
  }

  public List<MemberTypingStats> findByMemberId(Long memberId) {
    return em.createQuery(
            "select s from MemberTypingStats s where s.member.id = :memberId",
            MemberTypingStats.class)
        .setParameter("memberId", memberId)
        .getResultList();
  }

  public Optional<MemberTypingStats> findByMemberIdAndLanguage(
      Long memberId, QuoteLanguage language) {
    return em.createQuery(
            "select s from MemberTypingStats s where s.member.id = :memberId "
                + "and s.language = :language",
            MemberTypingStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .getResultStream()
        .findFirst();
  }
}
