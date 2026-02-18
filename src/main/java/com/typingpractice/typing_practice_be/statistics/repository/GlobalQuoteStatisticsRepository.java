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

  public Object[] aggregateByLanguage(QuoteLanguage language) {
    return (Object[])
        em.createNativeQuery(
                """
								SELECT
										COALESCE(AVG(length), 0), COALESCE(STDDEV_POP(length), 0),
										COALESCE(AVG(punc_rate), 0), COALESCE(STDDEV_POP(punc_rate), 0),
										COALESCE(AVG(space_rate), 0), COALESCE(STDDEV_POP(space_rate), 0),
										COALESCE(AVG(digit_rate), 0), COALESCE(STDDEV_POP(digit_rate), 0),
										COALESCE(AVG(jamo_complex), 0), COALESCE(STDDEV_POP(jamo_complex), 0),
										COALESCE(AVG(diphthong_rate), 0), COALESCE(STDDEV_POP(diphthong_rate), 0),
										COALESCE(AVG(shift_jamo_rate), 0), COALESCE(STDDEV_POP(shift_jamo_rate), 0),
										COALESCE(AVG(case_flip_rate), 0), COALESCE(STDDEV_POP(case_flip_rate), 0),
										COALESCE(AVG(avg_word_len), 0), COALESCE(STDDEV_POP(avg_word_len), 0)
								FROM quote
								WHERE language = :language AND deleted = false
								""")
            .setParameter("language", language.name())
            .getSingleResult();
  }
}
