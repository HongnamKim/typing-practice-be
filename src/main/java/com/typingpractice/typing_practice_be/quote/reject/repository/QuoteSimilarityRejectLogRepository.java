package com.typingpractice.typing_practice_be.quote.reject.repository;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.reject.domain.QuoteSimilarityRejectLog;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class QuoteSimilarityRejectLogRepository {
  private final EntityManager em;

  public void save(QuoteSimilarityRejectLog log) {
    em.persist(log);
  }

  public List<QuoteSimilarityRejectLog> findByPeriod(
      QuoteLanguage language, LocalDateTime from, LocalDateTime to, int page, int size) {
    return em.createQuery(
            "select r from QuoteSimilarityRejectLog r "
                + "where r.createdAt >= :from and r.createdAt <= :to "
                + "and r.language = :language "
                + "order by r.createdAt desc",
            QuoteSimilarityRejectLog.class)
        .setParameter("language", language)
        .setParameter("from", from)
        .setParameter("to", to)
        .setFirstResult((page - 1) * size)
        .setMaxResults(size + 1)
        .getResultList();
  }

  public long countByPeriod(QuoteLanguage language, LocalDateTime from, LocalDateTime to) {
    return em.createQuery(
            "select count(r) from QuoteSimilarityRejectLog r "
                + "where r.createdAt >= :from and r.createdAt <= :to "
                + "and r.language = :language",
            Long.class)
        .setParameter("from", from)
        .setParameter("to", to)
        .setParameter("language", language)
        .getSingleResult();
  }

  public Double avgSimilarityByPeriod(
      QuoteLanguage language, LocalDateTime from, LocalDateTime to) {
    return em.createQuery(
            "select avg(r.similarity) from QuoteSimilarityRejectLog r "
                + "where r.createdAt >= :from and r.createdAt <= :to "
                + "and r.language = :language",
            Double.class)
        .setParameter("from", from)
        .setParameter("to", to)
        .setParameter("language", language)
        .getSingleResult();
  }
}
