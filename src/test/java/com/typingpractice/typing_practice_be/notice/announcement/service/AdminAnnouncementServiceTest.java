package com.typingpractice.typing_practice_be.notice.announcement.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.announcement.domain.Announcement;
import com.typingpractice.typing_practice_be.notice.announcement.dto.*;
import com.typingpractice.typing_practice_be.notice.announcement.exception.AnnouncementNotFoundException;
import com.typingpractice.typing_practice_be.notice.announcement.repository.AnnouncementRepository;
import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.typingpractice.typing_practice_be.notice.dto.LocalizedTextRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AdminAnnouncementServiceTest {
  @Mock private AnnouncementRepository announcementRepository;

  @InjectMocks private AdminAnnouncementService adminAnnouncementService;

  private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 5, 10, 0);

  private Announcement createAnnouncement(
      Long id, LocalDateTime postedAt, boolean published, boolean pinned) {
    Announcement a =
        Announcement.create(
            postedAt,
            new LocalizedText("제목", null, null),
            new LocalizedText("내용", null, null),
            published,
            pinned);
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
  @DisplayName("create")
  class Create {
    @Test
    @DisplayName("성공 - 저장 후 id 반환")
    void success() {
      CreateAnnouncementRequest request =
          new CreateAnnouncementRequest(
              BASE_TIME,
              new LocalizedTextRequest("제목", null, null),
              new LocalizedTextRequest("내용", null, null),
              true,
              false);

      when(announcementRepository.save(any(Announcement.class)))
          .thenAnswer(
              invocation -> {
                Announcement saved = invocation.getArgument(0);
                setId(saved, 1L);
                return saved;
              });

      Announcement announcement = adminAnnouncementService.create(request);

      assertThat(announcement.getId()).isEqualTo(1L);
      verify(announcementRepository).save(any(Announcement.class));
    }
  }

  @Nested
  @DisplayName("update")
  class Update {
    @Test
    @DisplayName("성공 - 모든 필드 변경")
    void successAllFields() {
      Announcement existing = createAnnouncement(1L, BASE_TIME, true, false);
      LocalDateTime newTime = BASE_TIME.plusDays(1);
      UpdateAnnouncementRequest request =
          new UpdateAnnouncementRequest(
              newTime,
              new LocalizedTextRequest("새 제목", null, null),
              new LocalizedTextRequest("새 내용", null, null),
              false,
              true);

      when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));

      adminAnnouncementService.update(1L, request);

      assertThat(existing.getPostedAt()).isEqualTo(newTime);
      assertThat(existing.getTitle().ko()).isEqualTo("새 제목");
      assertThat(existing.getContent().ko()).isEqualTo("새 내용");
      assertThat(existing.isPublished()).isFalse();
      assertThat(existing.isPinned()).isTrue();
    }

    @Test
    @DisplayName("성공 - null 필드는 미변경")
    void successPartial() {
      Announcement existing = createAnnouncement(1L, BASE_TIME, true, false);
      LocalDateTime newTime = BASE_TIME.plusDays(1);
      UpdateAnnouncementRequest request =
          new UpdateAnnouncementRequest(newTime, null, null, null, null);

      when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));

      adminAnnouncementService.update(1L, request);

      assertThat(existing.getPostedAt()).isEqualTo(newTime);
      assertThat(existing.getTitle().ko()).isEqualTo("제목"); // 미변경
      assertThat(existing.getContent().ko()).isEqualTo("내용"); // 미변경
      assertThat(existing.isPublished()).isTrue(); // 미변경
      assertThat(existing.isPinned()).isFalse(); // 미변경
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      UpdateAnnouncementRequest request =
          new UpdateAnnouncementRequest(BASE_TIME, null, null, null, null);

      when(announcementRepository.findById(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> adminAnnouncementService.update(1L, request))
          .isInstanceOf(AnnouncementNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {
    @Test
    @DisplayName("성공 - Repository.delete 호출")
    void success() {
      Announcement existing = createAnnouncement(1L, BASE_TIME, true, false);
      when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));

      adminAnnouncementService.delete(1L);

      verify(announcementRepository).delete(existing);
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      when(announcementRepository.findById(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> adminAnnouncementService.delete(1L))
          .isInstanceOf(AnnouncementNotFoundException.class);
      verify(announcementRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("findOne")
  class FindOne {
    @Test
    @DisplayName("게시된 공지 - 정상 반환")
    void successPublished() {
      Announcement a = createAnnouncement(1L, BASE_TIME, true, false);
      when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));

      AnnouncementDetail result = adminAnnouncementService.findOne(1L);

      assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("미게시 공지 - 어드민은 정상 조회")
    void successUnpublished() {
      Announcement a = createAnnouncement(1L, BASE_TIME, false, false);
      when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));

      AnnouncementDetail result = adminAnnouncementService.findOne(1L);

      assertThat(result.id()).isEqualTo(1L);
      assertThat(result.published()).isFalse();
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      when(announcementRepository.findById(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> adminAnnouncementService.findOne(1L))
          .isInstanceOf(AnnouncementNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findPinned")
  class FindPinned {
    @Test
    @DisplayName("고정 공지 목록 반환 - 게시 여부 무관")
    void success() {
      Announcement published = createAnnouncement(1L, BASE_TIME, true, true);
      Announcement unpublished = createAnnouncement(2L, BASE_TIME.minusDays(1), false, true);
      when(announcementRepository.findAllPinned()).thenReturn(List.of(published, unpublished));

      List<AnnouncementSummary> result = adminAnnouncementService.findPinned();

      assertThat(result).hasSize(2);
      assertThat(result.get(0).id()).isEqualTo(1L);
      assertThat(result.get(1).id()).isEqualTo(2L);
      assertThat(result.get(1).published()).isFalse();
    }
  }

  @Nested
  @DisplayName("findList")
  class FindList {
    @Test
    @DisplayName("hasNext = true - 마지막 항목 잘라내고 nextCursor 발급")
    void hasNext() {
      Announcement a1 = createAnnouncement(1L, BASE_TIME, true, false);
      Announcement a2 = createAnnouncement(2L, BASE_TIME.minusDays(1), false, false);
      Announcement a3 = createAnnouncement(3L, BASE_TIME.minusDays(2), true, false);
      when(announcementRepository.findAllNonPinnedByCursor(null, 2))
          .thenReturn(List.of(a1, a2, a3));

      CursorPage<AnnouncementSummary, AnnouncementCursor> result =
          adminAnnouncementService.findList(null, 2);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent().get(0).id()).isEqualTo(1L);
      assertThat(result.getContent().get(1).id()).isEqualTo(2L);
      assertThat(result.isHasNext()).isTrue();
      assertThat(result.getNextCursor()).isNotNull();
      assertThat(result.getNextCursor().id()).isEqualTo(2L);
      assertThat(result.getNextCursor().postedAt()).isEqualTo(BASE_TIME.minusDays(1));
    }

    @Test
    @DisplayName("hasNext = false - nextCursor null")
    void noNext() {
      Announcement a1 = createAnnouncement(1L, BASE_TIME, false, false);
      Announcement a2 = createAnnouncement(2L, BASE_TIME.minusDays(1), true, false);
      when(announcementRepository.findAllNonPinnedByCursor(null, 2)).thenReturn(List.of(a1, a2));

      CursorPage<AnnouncementSummary, AnnouncementCursor> result =
          adminAnnouncementService.findList(null, 2);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.isHasNext()).isFalse();
      assertThat(result.getNextCursor()).isNull();
    }
  }
}
