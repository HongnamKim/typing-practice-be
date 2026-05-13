package com.typingpractice.typing_practice_be.notice.update.repository;

import com.typingpractice.typing_practice_be.notice.update.domain.UpdateNote;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteCursor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UpdateNoteRepository {
  private final EntityManager em;

  public UpdateNote save(UpdateNote updateNote) {
    em.persist(updateNote);
    return updateNote;
  }

  public Optional<UpdateNote> findById(Long id) {
    return em.createQuery("select u from UpdateNote u where u.id = :id", UpdateNote.class)
        .setParameter("id", id)
        .getResultStream()
        .findFirst();
  }

  public void delete(UpdateNote updateNote) {
    em.remove(updateNote);
  }

  /** version 중복 체크. 자기 자신 제외 옵션 (수정 시 사용). */
  public boolean existsByVersion(String version, Long excludeId) {
    StringBuilder jpql =
        new StringBuilder("select count(u) from UpdateNote u where u.version = :version");
    if (excludeId != null) {
      jpql.append(" and u.id <> :excludeId");
    }
    TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
    query.setParameter("version", version);
    if (excludeId != null) {
      query.setParameter("excludeId", excludeId);
    }
    return query.getSingleResult() > 0;
  }

  /** 사용자: 게시된 업데이트 노트 중 가장 최신 1건. 팝업용. */
  public Optional<UpdateNote> findLatestPublished() {
    return em.createQuery(
            "select u from UpdateNote u "
                + "where u.published = true "
                + "order by u.releasedAt desc, u.id desc",
            UpdateNote.class)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  /** 사용자: 게시된 업데이트 노트 커서 페이지네이션. size+1 조회로 hasNext 판정용. */
  public List<UpdateNote> findPublishedByCursor(UpdateNoteCursor cursor, int size) {
    return findByCursor(cursor, size, true);
  }

  /** 어드민: 업데이트 노트 커서 페이지네이션 (게시 여부 무관). size+1 조회로 hasNext 판정용. */
  public List<UpdateNote> findAllByCursor(UpdateNoteCursor cursor, int size) {
    return findByCursor(cursor, size, false);
  }

  private List<UpdateNote> findByCursor(UpdateNoteCursor cursor, int size, boolean publishedOnly) {
    StringBuilder jpql = new StringBuilder("select u from UpdateNote u");
    boolean hasWhere = false;
    if (publishedOnly) {
      jpql.append(" where u.published = true");
      hasWhere = true;
    }
    if (cursor != null) {
      jpql.append(hasWhere ? " and" : " where");
      jpql.append(
          " (u.releasedAt < :cursorReleasedAt"
              + "      or (u.releasedAt = :cursorReleasedAt and u.id < :cursorId))");
    }
    jpql.append(" order by u.releasedAt desc, u.id desc");

    TypedQuery<UpdateNote> query = em.createQuery(jpql.toString(), UpdateNote.class);
    if (cursor != null) {
      query.setParameter("cursorReleasedAt", cursor.releasedAt());
      query.setParameter("cursorId", cursor.id());
    }
    return query.setMaxResults(size + 1).getResultList();
  }
}
