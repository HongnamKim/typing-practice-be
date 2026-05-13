package com.typingpractice.typing_practice_be.notice.update.service;

import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.notice.update.domain.UpdateNote;
import com.typingpractice.typing_practice_be.notice.update.dto.CreateUpdateNoteRequest;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteCursor;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteResponse;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateUpdateNoteRequest;
import com.typingpractice.typing_practice_be.notice.update.exception.UpdateNoteNotFoundException;
import com.typingpractice.typing_practice_be.notice.update.exception.UpdateNoteVersionConflictException;
import com.typingpractice.typing_practice_be.notice.update.repository.UpdateNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUpdateNoteService {
  private final UpdateNoteRepository updateNoteRepository;

  @Transactional
  public UpdateNote create(CreateUpdateNoteRequest request) {
    if (updateNoteRepository.existsByVersion(request.version(), null)) {
      throw new UpdateNoteVersionConflictException();
    }

    LocalDateTime releasedAtUtc = TimeUtils.kstToUtc(request.releasedAt());

    UpdateNote note =
        UpdateNote.create(
            request.version(),
            releasedAtUtc,
            request.newFeaturesAsValue(),
            request.improvementsAsValue(),
            request.published());
    return updateNoteRepository.save(note);
  }

  @Transactional
  public UpdateNote update(Long id, UpdateUpdateNoteRequest request) {
    UpdateNote note =
        updateNoteRepository.findById(id).orElseThrow(UpdateNoteNotFoundException::new);

    if (request.version() != null && updateNoteRepository.existsByVersion(request.version(), id)) {
      throw new UpdateNoteVersionConflictException();
    }

    LocalDateTime releasedAtUtc =
        request.releasedAt() == null ? null : TimeUtils.kstToUtc(request.releasedAt());

    note.update(
        request.version(),
        releasedAtUtc,
        request.newFeaturesAsValue(),
        request.improvementsAsValue(),
        request.published());

    return note;
  }

  @Transactional
  public void delete(Long id) {
    UpdateNote note =
        updateNoteRepository.findById(id).orElseThrow(UpdateNoteNotFoundException::new);
    updateNoteRepository.delete(note);
  }

  public UpdateNoteResponse findOne(Long id) {
    return updateNoteRepository
        .findById(id)
        .map(UpdateNoteResponse::from)
        .orElseThrow(UpdateNoteNotFoundException::new);
  }

  /** 어드민: 업데이트 노트 커서 페이지네이션 (게시 여부 무관). */
  public CursorPage<UpdateNoteResponse, UpdateNoteCursor> findList(
      UpdateNoteCursor cursor, int size) {
    List<UpdateNote> fetched = updateNoteRepository.findAllByCursor(cursor, size);

    boolean hasNext = fetched.size() > size;
    List<UpdateNote> page = hasNext ? fetched.subList(0, size) : fetched;

    List<UpdateNoteResponse> content = page.stream().map(UpdateNoteResponse::from).toList();

    UpdateNoteCursor nextCursor =
        hasNext
            ? new UpdateNoteCursor(page.getLast().getReleasedAt(), page.getLast().getId())
            : null;

    return new CursorPage<>(content, nextCursor, hasNext);
  }
}
