package com.typingpractice.typing_practice_be.quote.repository;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuoteRepository {
  private final EntityManager em;

  public void save(Quote quote) {
    em.persist(quote);
  }

  public Optional<Quote> findById(Long quoteId) {
    return Optional.ofNullable(em.find(Quote.class, quoteId));
  }

  public List<Quote> findAll() {
    return em.createQuery("select q from Quote q", Quote.class).getResultList();
  }

  public List<Quote> findByStatus(QuoteStatus quoteStatus) {
    return em.createQuery("select q from Quote q where q.status = :status", Quote.class)
        .setParameter("status", quoteStatus)
        .getResultList();
  }

  public void deleteQuote(Quote quote) {
    em.remove(quote);
  }
}
