package com.typingpractice.typing_practice_be.notice.announcement.repository;

import com.typingpractice.typing_practice_be.notice.announcement.domain.Announcement;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AnnouncementRepository {
  private final EntityManager em;

  public Announcement save(Announcement announcement) {
    em.persist(announcement);
    return announcement;
  }

  public Optional<Announcement> findById(Long id) {
    return Optional.ofNullable(em.find(Announcement.class, id));
  }

  public Optional<Announcement> findLatestPublished() {
    return em.createQuery(
            "select a from Announcement a "
                + "where a.published = true "
                + "order by a.postedAt desc",
            Announcement.class)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  public List<Announcement> findPublishedRecent(int limit) {
    return em.createQuery(
            "select a from Announcement a "
                + "where a.published = true "
                + "order by a.postedAt desc",
            Announcement.class)
        .setMaxResults(limit)
        .getResultList();
  }

  public List<Announcement> findAllForAdmin(int page, int size) {
    return em.createQuery(
            "select a from Announcement a order by a.postedAt desc", Announcement.class)
        .setFirstResult((page - 1) * size)
        .setMaxResults(size)
        .getResultList();
  }

  public void delete(Announcement announcement) {
    em.remove(announcement);
  }
}
