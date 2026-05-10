package com.typingpractice.typing_practice_be.notice.announcement.service;

import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.announcement.domain.Announcement;
import com.typingpractice.typing_practice_be.notice.announcement.dto.*;
import com.typingpractice.typing_practice_be.notice.announcement.exception.AnnouncementNotFoundException;
import com.typingpractice.typing_practice_be.notice.announcement.repository.AnnouncementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnnouncementService {
  private final AnnouncementRepository announcementRepository;

  @Transactional
  public Announcement create(CreateAnnouncementRequest request) {

    Announcement announcement =
        Announcement.create(
            request.postedAt(),
            request.titleAsValue(),
            request.contentAsValue(),
            request.published(),
            request.pinned());

    return announcementRepository.save(announcement);
  }

  @Transactional
  public Announcement update(Long id, UpdateAnnouncementRequest request) {
    Announcement announcement =
        announcementRepository.findById(id).orElseThrow(AnnouncementNotFoundException::new);

    announcement.update(
        request.postedAt(),
        request.titleAsValue(),
        request.contentAsValue(),
        request.published(),
        request.pinned());

    return announcement;
  }

  @Transactional
  public void delete(Long id) {
    Announcement announcement =
        announcementRepository.findById(id).orElseThrow(AnnouncementNotFoundException::new);

    announcementRepository.delete(announcement);
  }

  public AnnouncementDetail findOne(Long id) {
    return announcementRepository
        .findById(id)
        .map(AnnouncementDetail::from)
        .orElseThrow(AnnouncementNotFoundException::new);
  }

  public List<AnnouncementSummary> findPinned() {
    return announcementRepository.findAllPinned().stream().map(AnnouncementSummary::from).toList();
  }

  /** 어드민: 일반 공지 커서 페이지네이션 (게시 여부 무관). */
  public CursorPage<AnnouncementSummary, AnnouncementCursor> findList(
      AnnouncementCursor cursor, int size) {
    List<Announcement> fetched = announcementRepository.findAllNonPinnedByCursor(cursor, size);

    boolean hasNext = fetched.size() > size;
    List<Announcement> page = hasNext ? fetched.subList(0, size) : fetched;

    List<AnnouncementSummary> content = page.stream().map(AnnouncementSummary::from).toList();

    AnnouncementCursor nextCursor =
        hasNext
            ? new AnnouncementCursor(page.getLast().getPostedAt(), page.getLast().getId())
            : null;

    return new CursorPage<>(content, nextCursor, hasNext);
  }
}
