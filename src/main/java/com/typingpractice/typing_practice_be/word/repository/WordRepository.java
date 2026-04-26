package com.typingpractice.typing_practice_be.word.repository;

import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.query.WordPaginationQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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

  public List<Object[]> findAllIdsWithDifficulty(WordLanguage language) {
    return em.createQuery(
            "select w.id, w.difficulty from Word w where w.language = :language order by w.difficulty asc",
            Object[].class)
        .setParameter("language", language)
        .getResultList();
  }

  public List<Long> findAllIdsSortedByDifficulty(WordLanguage language) {
    return em.createQuery(
            "select w.id from Word w where w.language = :language order by w.difficulty asc",
            Long.class)
        .setParameter("language", language)
        .getResultList();
  }

  public Optional<Word> findByIdWithTypingStats(Long wordId) {
    return em.createQuery(
            "select w from Word w left join fetch w.typingStats where w.id = :wordId", Word.class)
        .setParameter("wordId", wordId)
        .getResultStream()
        .findFirst();
  }

  public List<Word> findAll(WordPaginationQuery query) {
    int page = query.getPage();
    int size = query.getSize();

    String jpql = "select w from Word w left join fetch w.typingStats";

    if (query.getLanguage() != null) {
      jpql += " where w.language = :language";
    }

    jpql += " order by w." + query.getOrderBy() + " " + query.getSortDirection();

    TypedQuery<Word> typedQuery =
        em.createQuery(jpql, Word.class).setFirstResult((page - 1) * size).setMaxResults(size + 1);

    if (query.getLanguage() != null) {
      typedQuery.setParameter("language", query.getLanguage());
    }

    return typedQuery.getResultList();
  }
}
