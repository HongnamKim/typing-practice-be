package com.typingpractice.typing_practice_be.word.repository;

import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WordRepository {
  private final EntityManager em;

  public void save(Word word) {
    em.persist(word);
  }

  public Optional<Word> findById(Long id) {
    return Optional.ofNullable(em.find(Word.class, id));
  }

  public List<Word> findByIds(List<Long> ids, WordLanguage language) {
    if (ids.isEmpty()) return List.of();

    return em.createQuery(
            "select w from Word w where w.id in :ids and w.language = :language", Word.class)
        .setParameter("ids", ids)
        .setParameter("language", language)
        .getResultList();
  }

  public List<Long> findAllIds(WordLanguage language) {
    return em.createQuery("select w.id from Word w where w.language = :language", Long.class)
        .setParameter("language", language)
        .getResultList();
  }

  public void deleteWord(Word w) {
    em.remove(w);
  }

  public Long findMaxIdByLanguage(WordLanguage language) {
    return em.createQuery("select max(w.id) from Word w where w.language = :language", Long.class)
        .setParameter("language", language)
        .getSingleResult();
  }

  public List<Word> findPageByLanguageAndIdRange(
      WordLanguage language, Long cursorId, Long maxId, int size) {
    return em.createQuery(
            "select w from Word w where w.language = :language and w.id > :cursorId and w.id <= :maxId order by w.id ASC",
            Word.class)
        .setParameter("language", language)
        .setParameter("cursorId", cursorId)
        .setParameter("maxId", maxId)
        .setMaxResults(size)
        .getResultList();
  }
}
