package com.typingpractice.typing_practice_be.typingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
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
}
