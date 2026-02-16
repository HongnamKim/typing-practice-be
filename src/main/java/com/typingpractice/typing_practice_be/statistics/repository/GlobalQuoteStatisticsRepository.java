package com.typingpractice.typing_practice_be.statistics.repository;

import com.typingpractice.typing_practice_be.statistics.domain.GlobalQuoteStatistics;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GlobalQuoteStatisticsRepository {
  private final EntityManager em;

  public List<GlobalQuoteStatistics> findAll() {
    return em.createQuery("select s from GlobalQuoteStatistics", GlobalQuoteStatistics.class)
        .setMaxResults(50)
        .getResultList();
  }
}
