package com.typingpractice.typing_practice_be.word.statistics.repository;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.statistics.domain.GlobalWordStatistics;
import com.typingpractice.typing_practice_be.word.statistics.dto.WordProfileAggregation;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GlobalWordStatisticsRepository {
  private final EntityManager em;

  public void save(GlobalWordStatistics stats) {
    em.persist(stats);
  }

  public Optional<GlobalWordStatistics> findByLanguage(WordLanguage language) {
    return em.createQuery(
            "select g from GlobalWordStatistics g where g.language = :language",
            GlobalWordStatistics.class)
        .setParameter("language", language)
        .getResultStream()
        .findFirst();
  }

  public WordProfileAggregation aggregateByLanguage(WordLanguage language) {
    Object[] row =
        (Object[])
            em.createQuery(
                    "select "
                        + "avg(w.profile.length), cast(stddev(w.profile.length) as float), "
                        + "avg(w.profile.jamoComplex), cast(stddev(w.profile.jamoComplex) as float), "
                        + "avg(w.profile.diphthongRate), cast(stddev(w.profile.diphthongRate) as float), "
                        + "avg(w.profile.shiftJamoRate), cast(stddev(w.profile.shiftJamoRate) as float), "
                        + "avg(w.profile.caseFlipRate), cast(stddev(w.profile.caseFlipRate) as float) "
                        + "from Word w where w.language = :language")
                .setParameter("language", language)
                .getSingleResult();

    return WordProfileAggregation.from(row);
  }
}
