package com.typingpractice.typing_practice_be.notice.update.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import com.typingpractice.typing_practice_be.notice.dto.LocalizedTextRequest;
import com.typingpractice.typing_practice_be.notice.update.domain.UpdateNote;
import com.typingpractice.typing_practice_be.notice.update.dto.CreateUpdateNoteRequest;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteCursor;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteResponse;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateUpdateNoteRequest;
import com.typingpractice.typing_practice_be.notice.update.exception.UpdateNoteNotFoundException;
import com.typingpractice.typing_practice_be.notice.update.exception.UpdateNoteVersionConflictException;
import com.typingpractice.typing_practice_be.notice.update.repository.UpdateNoteRepository;
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
class AdminUpdateNoteServiceTest {
  @Mock private UpdateNoteRepository updateNoteRepository;

  @InjectMocks private AdminUpdateNoteService adminUpdateNoteService;

  private static final LocalDateTime KST_TIME = LocalDateTime.of(2026, 5, 9, 15, 0);
  private static final LocalDateTime EXPECTED_UTC = TimeUtils.kstToUtc(KST_TIME);

  private UpdateNote createUpdateNote(
      Long id, String version, LocalDateTime releasedAt, boolean published) {
    UpdateNote note =
        UpdateNote.create(
            version,
            releasedAt,
            List.of(new LocalizedText("새 기능", null, null)),
            List.of(new LocalizedText("개선", null, null)),
            published);

    setId(note, id);
    return note;
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
    @DisplayName("성공 - KST를 UTC로 변환하여 저장")
    void successKstToUtc() {
      CreateUpdateNoteRequest request =
          new CreateUpdateNoteRequest(
              "v1.0.0",
              KST_TIME,
              List.of(new LocalizedTextRequest("새 기능", null, null)),
              List.of(new LocalizedTextRequest("개선", null, null)),
              true);

      when(updateNoteRepository.existsByVersion("v1.0.0", null)).thenReturn(false);
      when(updateNoteRepository.save(any(UpdateNote.class)))
          .thenAnswer(
              invocation -> {
                UpdateNote saved = invocation.getArgument(0);
                setId(saved, 1L);
                return saved;
              });

      UpdateNote result = adminUpdateNoteService.create(request);

      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getReleasedAt()).isEqualTo(EXPECTED_UTC);
    }

    @Test
    @DisplayName("version 충돌 - 예외 발생, save 호출 안 함")
    void versionConflict() {
      CreateUpdateNoteRequest request =
          new CreateUpdateNoteRequest("v1.0.0", KST_TIME, List.of(), List.of(), true);

      when(updateNoteRepository.existsByVersion("v1.0.0", null)).thenReturn(true);

      assertThatThrownBy(() -> adminUpdateNoteService.create(request))
          .isInstanceOf(UpdateNoteVersionConflictException.class);
      verify(updateNoteRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("update")
  class Update {
    @Test
    @DisplayName("성공 - 모든 필드 변경 + KST를 UTC로 변환")
    void successAllFields() {
      UpdateNote existing =
          createUpdateNote(1L, "v1.0.0", LocalDateTime.of(2026, 1, 1, 0, 0), true);

      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest(
              "v1.0.1",
              KST_TIME,
              List.of(new LocalizedTextRequest("새 기능 변경", null, null)),
              List.of(new LocalizedTextRequest("개선 변경", null, null)),
              false);

      when(updateNoteRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(updateNoteRepository.existsByVersion("v1.0.1", 1L)).thenReturn(false);

      UpdateNote updated = adminUpdateNoteService.update(1L, request);

      assertThat(updated.getVersion()).isEqualTo("v1.0.1");
      assertThat(updated.getReleasedAt()).isEqualTo(EXPECTED_UTC);
      assertThat(updated.getNewFeatures().getFirst().ko()).isEqualTo("새 기능 변경");
      assertThat(updated.getImprovements().getFirst().ko()).isEqualTo("개선 변경");
      assertThat(updated.isPublished()).isFalse();
    }

    @Test
    @DisplayName("성공 - null 필드 미변경")
    void successPartial() {
      UpdateNote existing =
          createUpdateNote(1L, "v1.0.0", LocalDateTime.of(2026, 1, 1, 0, 0), true);
      // published만 변경
      UpdateUpdateNoteRequest request = new UpdateUpdateNoteRequest(null, null, null, null, false);

      when(updateNoteRepository.findById(1L)).thenReturn(Optional.of(existing));

      UpdateNote updated = adminUpdateNoteService.update(1L, request);

      assertThat(updated.getVersion()).isEqualTo("v1.0.0"); // 미변경
      assertThat(updated.getReleasedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0)); // 미변경
      assertThat(updated.getNewFeatures().getFirst().ko()).isEqualTo("새 기능"); // 미변경
      assertThat(updated.isPublished()).isFalse(); // 변경됨
    }

    @Test
    @DisplayName("version null - 충돌 체크 스킵")
    void skipVersionCheckWhenNull() {
      UpdateNote existing =
          createUpdateNote(1L, "v1.0.0", LocalDateTime.of(2026, 1, 1, 0, 0), true);
      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest(null, KST_TIME, null, null, null);

      when(updateNoteRepository.findById(1L)).thenReturn(Optional.of(existing));

      adminUpdateNoteService.update(1L, request);

      verify(updateNoteRepository, never()).existsByVersion(any(), any());
    }

    @Test
    @DisplayName("version 변경 - 자기 자신 제외하고 충돌 체크")
    void versionCheckExcludeSelf() {
      UpdateNote existing =
          createUpdateNote(1L, "v1.0.0", LocalDateTime.of(2026, 1, 1, 0, 0), true);
      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest("v1.0.1", null, null, null, null);

      when(updateNoteRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(updateNoteRepository.existsByVersion("v1.0.1", 1L)).thenReturn(false);

      adminUpdateNoteService.update(1L, request);

      verify(updateNoteRepository).existsByVersion("v1.0.1", 1L);
    }

    @Test
    @DisplayName("version 충돌 - 예외 발생")
    void versionConflict() {
      UpdateNote existing =
          createUpdateNote(1L, "v1.0.0", LocalDateTime.of(2026, 1, 1, 0, 0), true);
      UpdateUpdateNoteRequest request =
          new UpdateUpdateNoteRequest("v1.0.1", null, null, null, null);

      when(updateNoteRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(updateNoteRepository.existsByVersion("v1.0.1", 1L)).thenReturn(true);

      assertThatThrownBy(() -> adminUpdateNoteService.update(1L, request))
          .isInstanceOf(UpdateNoteVersionConflictException.class);
    }

    @Test
    @DisplayName("미존재 - 404")
    void notFound() {
      UpdateUpdateNoteRequest request = new UpdateUpdateNoteRequest(null, null, null, null, true);

      when(updateNoteRepository.findById(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> adminUpdateNoteService.update(1L, request))
          .isInstanceOf(UpdateNoteNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findList")
  class FindList {
    @Test
    @DisplayName("hasNext = true - 마지막 항목 잘라내고 nextCursor 발급")
    void hasNext() {
      LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
      UpdateNote n1 = createUpdateNote(1L, "v1.0.2", base.plusDays(2), true);
      UpdateNote n2 = createUpdateNote(2L, "v1.0.1", base.plusDays(1), false);
      UpdateNote n3 = createUpdateNote(3L, "v1.0.0", base, true);

      // size=2 요청, repository는 size+1=3개 반환
      when(updateNoteRepository.findAllByCursor(null, 2)).thenReturn(List.of(n1, n2, n3));

      CursorPage<UpdateNoteResponse, UpdateNoteCursor> result =
          adminUpdateNoteService.findList(null, 2);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.isHasNext()).isTrue();
      assertThat(result.getNextCursor()).isNotNull();
      assertThat(result.getNextCursor().id()).isEqualTo(2L);
      assertThat(result.getNextCursor().releasedAt()).isEqualTo(base.plusDays(1));
    }

    @Test
    @DisplayName("hasNext = false - nextCursor null")
    void noNext() {
      LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
      UpdateNote n1 = createUpdateNote(1L, "v1.0.1", base.plusDays(1), true);
      UpdateNote n2 = createUpdateNote(2L, "v1.0.0", base, true);

      when(updateNoteRepository.findAllByCursor(null, 2)).thenReturn(List.of(n1, n2));

      CursorPage<?, UpdateNoteCursor> result = adminUpdateNoteService.findList(null, 2);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.isHasNext()).isFalse();
      assertThat(result.getNextCursor()).isNull();
    }
  }
}
