package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypoDetailStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberWordTypoDetailStatsRepository {
  private final EntityManager em;

  public void save(MemberWordTypoDetailStats stats) {
    em.persist(stats);
  }

  public Optional<MemberWordTypoDetailStats> findByMemberIdAndLanguageAndExpectedAndActual(
      Long memberId, WordLanguage language, String expected, String actual) {
    return em.createQuery(
            "select s from MemberWordTypoDetailStats s "
                + "where s.member.id = :memberId "
                + "and s.language = :language "
                + "and s.expected = :expected "
                + "and s.actual = :actual",
            MemberWordTypoDetailStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .setParameter("expected", expected)
        .setParameter("actual", actual)
        .getResultStream()
        .findFirst();
  }

  public List<MemberWordTypoDetailStats> findByMemberIdAndLanguageAndExpected(
      Long memberId, WordLanguage language, String expected) {
    return em.createQuery(
            "select s from MemberWordTypoDetailStats s "
                + "where s.member.id = :memberId "
                + "and s.language = :language "
                + "and s.expected = :expected "
                + "order by s.typoCount desc",
            MemberWordTypoDetailStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .setParameter("expected", expected)
        .getResultList();
  }

  public void deleteAllInBatch() {
    em.createQuery("delete from MemberWordTypoDetailStats").executeUpdate();
  }
}
