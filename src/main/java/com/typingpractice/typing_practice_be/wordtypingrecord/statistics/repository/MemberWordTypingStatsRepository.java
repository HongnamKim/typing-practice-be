package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypingStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberWordTypingStatsRepository {
  private final EntityManager em;

  public void save(MemberWordTypingStats stats) {
    em.persist(stats);
  }

  public Optional<MemberWordTypingStats> findByMemberIdAndLanguage(
      Long memberId, WordLanguage language) {
    return em.createQuery(
            "select s from MemberWordTypingStats s where s.member.id = :memberId and s.language = :language",
            MemberWordTypingStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .getResultStream()
        .findFirst();
  }
}
