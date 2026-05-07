package com.typingpractice.typing_practice_be.notice.announcement.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.announcement.domain.Announcement;
import com.typingpractice.typing_practice_be.notice.announcement.dto.AnnouncementCursor;
import com.typingpractice.typing_practice_be.notice.announcement.dto.AnnouncementDetail;
import com.typingpractice.typing_practice_be.notice.announcement.dto.AnnouncementSummary;
import com.typingpractice.typing_practice_be.notice.announcement.exception.AnnouncementNotFoundException;
import com.typingpractice.typing_practice_be.notice.announcement.repository.AnnouncementRepository;
import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AnnouncementServiceTest {
  @Mock private AnnouncementRepository announcementRepository;

  @InjectMocks private AnnouncementService announcementService;

  private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 5, 10, 0);

  private Announcement createAnnouncement(Long id, LocalDateTime postedAt, boolean published) {
    Announcement a =
        Announcement.create(
            postedAt,
            new LocalizedText("제목", null, null),
            new LocalizedText("내용", null, null),
            published,
            false);
    setId(a, id);
    return a;
  }

  private void setId(Object entity, Long id) {
    try {
      Field idField = entity.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nested
  @DisplayName("findLatest")
  class FindLatest {
    @Test
    @DisplayName("최신 게시 공지 반환")
    void success() {
      Announcement latest = createAnnouncement(1L, BASE_TIME, true);

      when(announcementRepository.findLatestPublished()).thenReturn(Optional.of(latest));

      AnnouncementDetail result = announcementService.findLatest();

      assertThat(result).isNotNull();
      assertThat(result.id()).isEqualTo(latest.getId());
    }

    @Test
    @DisplayName("게시된 공지 없음 - null 반환")
    void empty() {
      when(announcementRepository.findLatestPublished()).thenReturn(Optional.empty());

      AnnouncementDetail result = announcementService.findLatest();

      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("findOne")
  class FindOne {
    @Test
    @DisplayName("게시된 공지 - 정상 반환")
    void success() {
      Announcement a = createAnnouncement(1L, BASE_TIME, true);
      when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));

      AnnouncementDetail result = announcementService.findOne(1L);

      assertThat(result.id()).isEqualTo(a.getId());
    }

    @Test
    @DisplayName("미게시 공지 - 404")
    void unpublished() {
      Announcement a = createAnnouncement(1L, BASE_TIME, false);
      when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));

      assertThatThrownBy(() -> announcementService.findOne(1L))
          .isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      when(announcementRepository.findById(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> announcementService.findOne(1L))
          .isInstanceOf(AnnouncementNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findPinned")
  class FindPinned {
    @Test
    @DisplayName("게시된 고정 공지 목록 반환")
    void success() {
      Announcement a1 = createAnnouncement(1L, BASE_TIME, true);
      Announcement a2 = createAnnouncement(2L, BASE_TIME.minusDays(1), true);

      when(announcementRepository.findPublishedPinned()).thenReturn(List.of(a1, a2));

      List<AnnouncementSummary> result = announcementService.findPinned();

      assertThat(result).hasSize(2);
      assertThat(result.getFirst().id()).isEqualTo(1L);
      assertThat(result.get(1).id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("고정 공지 없음 - 빈 목록")
    void empty() {
      when(announcementRepository.findPublishedPinned()).thenReturn(List.of());

      List<AnnouncementSummary> result = announcementService.findPinned();

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("findList")
  class FindList {
    @Test
    @DisplayName("hasNext = true - 마지막 항목 잘라내고 nextCursor 발급")
    void hasNext() {
      Announcement a1 = createAnnouncement(1L, BASE_TIME, true);
      Announcement a2 = createAnnouncement(2L, BASE_TIME.minusDays(1), true);
      Announcement a3 = createAnnouncement(3L, BASE_TIME.minusDays(2), true);
      when(announcementRepository.findPublishedNonPinnedByCursor(null, 2))
          .thenReturn(List.of(a1, a2, a3));

      CursorPage<AnnouncementSummary, AnnouncementCursor> result =
          announcementService.findList(null, 2);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent().getFirst().id()).isEqualTo(1L);
      assertThat(result.getContent().get(1).id()).isEqualTo(2L);
      assertThat(result.isHasNext()).isTrue();
      assertThat(result.getNextCursor()).isNotNull();
      assertThat(result.getNextCursor().id()).isEqualTo(2L);
      assertThat(result.getNextCursor().postedAt()).isEqualTo(BASE_TIME.minusDays(1));
    }

    @Test
    @DisplayName("hasNext = false - nextCursor null")
    void noNext() {
      // size=2 요청, repository는 2개 이하 반환 → hasNext=false
      Announcement a1 = createAnnouncement(1L, BASE_TIME, true);
      Announcement a2 = createAnnouncement(2L, BASE_TIME.minusDays(1), true);
      when(announcementRepository.findPublishedNonPinnedByCursor(null, 2))
          .thenReturn(List.of(a1, a2));

      CursorPage<AnnouncementSummary, AnnouncementCursor> result =
          announcementService.findList(null, 2);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.isHasNext()).isFalse();
      assertThat(result.getNextCursor()).isNull();
    }
  }
}
