package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberWordTypoStatsRepository {
  private final EntityManager em;

  public void save(MemberWordTypoStats stats) {
    em.persist(stats);
  }

  public Optional<MemberWordTypoStats> findByMemberIdAndLanguageAndExpected(
      Long memberId, WordLanguage language, String expected) {
    return em.createQuery(
            "select s from MemberWordTypoStats s "
                + "where s.member.id = :memberId "
                + "and s.language = :language "
                + "and s.expected = :expected",
            MemberWordTypoStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .setParameter("expected", expected)
        .getResultStream()
        .findFirst();
  }

  public List<MemberWordTypoStats> findTop10ByMemberIdAndLanguage(
      Long memberId, WordLanguage language) {
    return em.createQuery(
            "select s from MemberWordTypoStats s "
                + "where s.member.id = :memberId "
                + "and s.language = :language "
                + "order by s.typoCount desc",
            MemberWordTypoStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .setMaxResults(10)
        .getResultList();
  }

  public void deleteAllInBatch() {
    em.createQuery("delete from MemberWordTypoStats").executeUpdate();
  }
}
