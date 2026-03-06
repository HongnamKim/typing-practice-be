package com.typingpractice.typing_practice_be.typingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoDetailStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberTypoDetailStatsRepository {
  private final EntityManager em;

  public void save(MemberTypoDetailStats stats) {
    em.persist(stats);
  }

  public Optional<MemberTypoDetailStats> findByMemberIdAndLanguageAndExpectedAndActual(
      Long memberId, QuoteLanguage language, String expected, String actual) {
    return em.createQuery(
            "select s from MemberTypoDetailStats s "
                + "where s.member.id = :memberId "
                + "and s.language = :language "
                + "and s.expected = :expected "
                + "and s.actual = :actual",
            MemberTypoDetailStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .setParameter("expected", expected)
        .setParameter("actual", actual)
        .getResultStream()
        .findFirst();
  }

  public void deleteAllInBatch() {
    em.createQuery("delete from MemberTypoDetailStats").executeUpdate();
  }
}
