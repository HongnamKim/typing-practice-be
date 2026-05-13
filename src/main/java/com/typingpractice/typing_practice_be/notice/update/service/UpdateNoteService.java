package com.typingpractice.typing_practice_be.notice.update.service;

import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.update.domain.UpdateNote;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteCursor;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteResponse;
import com.typingpractice.typing_practice_be.notice.update.repository.UpdateNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UpdateNoteService {
  private final UpdateNoteRepository updateNoteRepository;

  public UpdateNoteResponse findLatest() {
    return updateNoteRepository.findLatestPublished().map(UpdateNoteResponse::from).orElse(null);
  }

  public CursorPage<UpdateNoteResponse, UpdateNoteCursor> findList(
      UpdateNoteCursor cursor, int size) {
    List<UpdateNote> fetched = updateNoteRepository.findPublishedByCursor(cursor, size);

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
