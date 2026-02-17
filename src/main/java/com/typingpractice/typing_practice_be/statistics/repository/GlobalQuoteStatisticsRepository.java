package com.typingpractice.typing_practice_be.statistics.repository;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.domain.GlobalQuoteStatistics;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GlobalQuoteStatisticsRepository {
  private final EntityManager em;

  public void save(GlobalQuoteStatistics stats) {
    em.persist(stats);
  }

  public Optional<GlobalQuoteStatistics> findTopByLanguageOrderByCreatedAtDesc(
      QuoteLanguage language) {
    List<GlobalQuoteStatistics> result =
        em.createQuery(
                "select s from GlobalQuoteStatistics s where s.language = :language order by s.createdAt DESC",
                GlobalQuoteStatistics.class)
            .setParameter("language", language)
            .setMaxResults(1)
            .getResultList();

    return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
  }

  public long count() {
    return em.createQuery("select count(s) from GlobalQuoteStatistics  s", Long.class)
        .getSingleResult();
  }
}
