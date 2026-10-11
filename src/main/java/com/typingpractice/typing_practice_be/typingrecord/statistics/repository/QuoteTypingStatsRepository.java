package com.typingpractice.typing_practice_be.typingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.PerfDistribution;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.QuoteTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.GlobalTypingPerformance;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
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
                        + "join s.quote q "
                        + "where s.language = :language and s.validAttemptsCount > 0 "
                        + "and q.type = :type "
                        + "and q.status = :status")
                .setParameter("language", language)
                .setParameter("type", QuoteType.PUBLIC)
                .setParameter("status", QuoteStatus.ACTIVE)
                .getSingleResult();

    if (row[0] == null) {
      return GlobalTypingPerformance.empty();
    }

    return GlobalTypingPerformance.of(
        ((Number) row[0]).floatValue(), ((Number) row[1]).floatValue());
  }

  /**
   * 공개·활성 문장 중 유효 시도 minValidAttempts 이상인 문장들의 perfRaw 평균·표준편차.
   * perfRaw = (1 - avgCpm/globalAvgCpm) + (1 - avgAcc/globalAvgAcc)
   */
  public PerfDistribution aggregatePerfDistributionByLanguage(
      QuoteLanguage language, float globalAvgCpm, float globalAvgAcc, int minValidAttempts) {
    Object[] row =
        (Object[])
            em.createNativeQuery(
                    """
                    SELECT COUNT(*), COALESCE(AVG(p), 0), COALESCE(STDDEV_POP(p), 0) FROM (
                      SELECT (1 - s.avg_cpm / :globalAvgCpm) + (1 - s.avg_acc / :globalAvgAcc) AS p
                      FROM quote_typing_stats s JOIN quote q ON q.quote_id = s.quote_id
                      WHERE s.language = :language AND s.deleted = false
                        AND s.valid_attempts_count >= :minAttempts
                        AND q.deleted = false AND q.type = 'PUBLIC' AND q.status = 'ACTIVE'
                    ) t
                    """)
                .setParameter("globalAvgCpm", globalAvgCpm)
                .setParameter("globalAvgAcc", globalAvgAcc)
                .setParameter("language", language.name())
                .setParameter("minAttempts", minValidAttempts)
                .getSingleResult();

    return PerfDistribution.of(
        ((Number) row[0]).longValue(),
        ((Number) row[1]).floatValue(),
        ((Number) row[2]).floatValue());
  }

  public List<QuoteTypingStats> findByLanguageWithQuote(
      QuoteLanguage language, int page, int size) {
    return em.createQuery(
            "select s from QuoteTypingStats s join fetch s.quote "
                + "where s.language = :language and s.validAttemptsCount > 0",
            QuoteTypingStats.class)
        .setParameter("language", language)
        .setFirstResult(page * size)
        .setMaxResults(size)
        .getResultList();
  }
}
