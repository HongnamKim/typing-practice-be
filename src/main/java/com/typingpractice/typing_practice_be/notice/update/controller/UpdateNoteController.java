package com.typingpractice.typing_practice_be.notice.update.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteCursor;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteCursorRequest;
import com.typingpractice.typing_practice_be.notice.update.dto.UpdateNoteResponse;
import com.typingpractice.typing_practice_be.notice.update.service.UpdateNoteService;
import java.awt.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/update-notes")
@RequiredArgsConstructor
public class UpdateNoteController {
  private final UpdateNoteService updateNoteService;

  @GetMapping
  public ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>> getUpdateNotes(
      @ModelAttribute @Valid UpdateNoteCursorRequest request) {
    CursorPage<UpdateNoteResponse, UpdateNoteCursor> result =
        updateNoteService.findList(request.toCursor(), request.sizeOrDefault());

    return ApiResponse.ok(result);
  }

  @GetMapping("/latest")
  public ApiResponse<UpdateNoteResponse> getLatestUpdateNote() {
    return ApiResponse.ok(updateNoteService.findLatest());
  }
}
