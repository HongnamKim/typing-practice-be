package com.typingpractice.typing_practice_be.notice.announcement.service;

import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.announcement.domain.Announcement;
import com.typingpractice.typing_practice_be.notice.announcement.dto.AnnouncementCursor;
import com.typingpractice.typing_practice_be.notice.announcement.dto.AnnouncementDetail;
import com.typingpractice.typing_practice_be.notice.announcement.dto.AnnouncementSummary;
import com.typingpractice.typing_practice_be.notice.announcement.exception.AnnouncementNotFoundException;
import com.typingpractice.typing_practice_be.notice.announcement.repository.AnnouncementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {
  private final AnnouncementRepository announcementRepository;

  public AnnouncementDetail findOne(Long id) {
    Announcement announcement =
        announcementRepository.findById(id).orElseThrow(AnnouncementNotFoundException::new);

    if (!announcement.isPublished()) {
      throw new AnnouncementNotFoundException();
    }

    return AnnouncementDetail.from(announcement);
  }

  /** 팝업용: 최신 게시 공지 1건 (없으면 null) */
  public AnnouncementDetail findLatest() {
    return announcementRepository.findLatestPublished().map(AnnouncementDetail::from).orElse(null);
  }

  /** 게시된 고정 공지 전체 */
  public List<AnnouncementSummary> findPinned() {
    return announcementRepository.findPublishedPinned().stream()
        .map(AnnouncementSummary::from)
        .toList();
  }

  /** 게시된 일반 공지 커서 페이지네이션 */
  public CursorPage<AnnouncementSummary, AnnouncementCursor> findList(
      AnnouncementCursor cursor, int size) {
    List<Announcement> fetched =
        announcementRepository.findPublishedNonPinnedByCursor(cursor, size);

    return toCursorPage(fetched, size);
  }

  private CursorPage<AnnouncementSummary, AnnouncementCursor> toCursorPage(
      List<Announcement> fetched, int size) {
    boolean hasNext = fetched.size() > size;
    List<Announcement> page = hasNext ? fetched.subList(0, size) : fetched;

    List<AnnouncementSummary> content = page.stream().map(AnnouncementSummary::from).toList();

    AnnouncementCursor nextCursor =
        hasNext
            ? new AnnouncementCursor(content.getLast().postedAt(), content.getLast().id())
            : null;

    return new CursorPage<>(content, nextCursor, hasNext);
  }
}
