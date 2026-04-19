package com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository;

import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.WordTypingStats;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WordTypingStatsRepository {
  private final EntityManager em;

  public void save(WordTypingStats stats) {
    em.persist(stats);
  }

  public Optional<WordTypingStats> findByWordId(Long wordId) {
    return em.createQuery(
            "select s from WordTypingStats s where s.word.id = :wordId", WordTypingStats.class)
        .setParameter("wordId", wordId)
        .getResultStream()
        .findFirst();
  }
}
