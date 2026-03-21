package com.typingpractice.typing_practice_be.typingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.GlobalTypingPerformance;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuoteTypingStatsRepository {
  private final EntityManager em;

  public void save(QuoteTypingStats stats) {
    em.persist(stats);
  }

  public Optional<QuoteTypingStats> findByQuoteId(Long quoteId) {
    return em.createQuery(
            "select s from QuoteTypingStats s where s.quote.id = :quoteId", QuoteTypingStats.class)
        .setParameter("quoteId", quoteId)
        .getResultStream()
        .findFirst();
  }

  public GlobalTypingPerformance aggregateGlobalAvgByLanguage(QuoteLanguage language) {
    Object[] row =
        (Object[])
            em.createQuery(
                    "select avg(s.avgCpm), avg(s.avgAcc) from QuoteTypingStats s "
                        + "where s.language = :language and s.validAttemptsCount > 0")
                .setParameter("language", language)
                .getSingleResult();

    if (row[0] == null) {
      return GlobalTypingPerformance.empty();
    }

    return GlobalTypingPerformance.of(
        ((Number) row[0]).floatValue(), ((Number) row[1]).floatValue());
  }
}
