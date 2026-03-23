package com.typingpractice.typing_practice_be.typingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypoStats;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberTypoStatsRepository {
  private final EntityManager em;

  public void save(MemberTypoStats stats) {
    em.persist(stats);
  }

  public Optional<MemberTypoStats> findByMemberIdAndLanguageAndExpected(
      Long memberId, QuoteLanguage language, String expected) {
    return em.createQuery(
            "select s from MemberTypoStats s "
                + "where s.member.id = :memberId "
                + "and s.language = :language "
                + "and s.expected = :expected",
            MemberTypoStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .setParameter("expected", expected)
        .getResultStream()
        .findFirst();
  }

  public List<MemberTypoStats> findTop10ByMemberIdAndLanguage(
      Long memberId, QuoteLanguage language) {
    return em.createQuery(
            "select s from MemberTypoStats s "
                + "where s.member.id = :memberId "
                + "and s.language = :language "
                + "order by s.typoCount desc",
            MemberTypoStats.class)
        .setParameter("memberId", memberId)
        .setParameter("language", language)
        .setMaxResults(10)
        .getResultList();
  }

  public void deleteAllInBatch() {
    em.createQuery("delete from MemberTypoStats").executeUpdate();
  }
}
