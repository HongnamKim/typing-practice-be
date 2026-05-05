package com.typingpractice.typing_practice_be.notice.announcement.repository;

import com.typingpractice.typing_practice_be.notice.announcement.domain.Announcement;
import com.typingpractice.typing_practice_be.notice.announcement.dto.AnnouncementCursor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
		return em.createQuery("select a from Announcement a where a.id = :id", Announcement.class)
						.setParameter("id", id)
						.getResultStream()
						.findFirst();
	}

	public void delete(Announcement announcement) {
		em.remove(announcement);
	}

	// 사용자: 게시된 공지 중 가장 최신 1건 (pinned 무관). 팝업용.
	public Optional<Announcement> findLatestPublished() {
		return em.createQuery(
										"select a from Announcement a "
														+ "where a.published = true "
														+ "order by a.postedAt desc, a.id desc",
										Announcement.class)
						.setMaxResults(1)
						.getResultStream()
						.findFirst();
	}

	/**
	 * 사용자: 게시된 고정 공지 전체
	 */
	public List<Announcement> findPublishedPinned() {
		return em.createQuery(
										"select a from Announcement a "
														+ "where a.published = true and a.pinned = true "
														+ "order by a.postedAt desc, a.id desc",
										Announcement.class)
						.getResultList();
	}

	public List<Announcement> findAllPinned() {
		return em.createQuery(
						"select a from Announcement a " +
										"where a.pinned = true " +
										"order by a.postedAt, a.id desc", Announcement.class
		).getResultList();
	}

	public List<Announcement> findPublishedNonPinnedByCursor(AnnouncementCursor cursor, int size) {
		return findByCursor(cursor, size, true);
	}

	public List<Announcement> findAllNonPinnedByCursor(AnnouncementCursor cursor, int size) {
		return findByCursor(cursor, size, false);
	}

	private List<Announcement> findByCursor(AnnouncementCursor cursor, int size, boolean publishedOnly) {
		StringBuilder jpql =
						new StringBuilder("select a from Announcement a where a.pinned = false");
		if (publishedOnly) {
			jpql.append(" and a.published = true");
		}
		if (cursor != null) {
			jpql.append(
							" and (a.postedAt < :cursorPostedAt" +
											" or (a.postedAt = :cursorPostedAt and a.id < :cursorId))"
			);
		}
		jpql.append(" order by a.postedAt desc, a.id desc");

		TypedQuery<Announcement> query = em.createQuery(jpql.toString(), Announcement.class);
		if (cursor != null) {
			query.setParameter("cursorPostedAt", cursor.postedAt());
			query.setParameter("cursorId", cursor.id());
		}
		return query.setMaxResults(size + 1).getResultList();
	}
}
