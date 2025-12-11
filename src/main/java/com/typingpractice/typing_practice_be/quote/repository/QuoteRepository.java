package com.typingpractice.typing_practice_be.quote.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
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
    return em.createQuery(
            "select q from Quote q join fetch q.member m where q.id = :quoteId", Quote.class)
        .setParameter("quoteId", quoteId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();

    // return Optional.ofNullable(em.find(Quote.class, quoteId));
  }

  public List<Quote> findAll() {
    return em.createQuery("select q from Quote q", Quote.class).getResultList();
  }

  public List<Quote> findPublicQuotes() {
    return em.createQuery(
            "select q from Quote q where q.status = :status and q.type = :type", Quote.class)
        .setParameter("status", QuoteStatus.ACTIVE)
        .setParameter("type", QuoteType.PUBLIC)
        .getResultList();
  }

  public List<Quote> findByStatus(QuoteStatus quoteStatus) {
    return em.createQuery("select q from Quote q where q.status = :status", Quote.class)
        .setParameter("status", quoteStatus)
        .getResultList();
  }

  public void deleteQuote(Quote quote) {
    em.remove(quote);
  }

  public List<Quote> findByMember(Member member) {
    return em.createQuery(
            "select q from Quote q join q.member m where m.id = :memberId", Quote.class)
        .setParameter("memberId", member.getId())
        .getResultList();
  }
}
