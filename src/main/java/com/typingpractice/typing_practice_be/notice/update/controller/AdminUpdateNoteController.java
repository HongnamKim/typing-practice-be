package com.typingpractice.typing_practice_be.notice.update.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.update.domain.UpdateNote;
import com.typingpractice.typing_practice_be.notice.update.dto.*;
import com.typingpractice.typing_practice_be.notice.update.service.AdminUpdateNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/update-notes")
@RequiredArgsConstructor
public class AdminUpdateNoteController {
  private final AdminUpdateNoteService adminUpdateNoteService;

  @PostMapping
  public ApiResponse<UpdateNoteResponse> postUpdateNote(
      @RequestBody @Valid CreateUpdateNoteRequest request) {
    UpdateNote note = adminUpdateNoteService.create(request);
    return ApiResponse.ok(UpdateNoteResponse.from(note));
  }

  @PatchMapping("/{id}")
  public ApiResponse<UpdateNoteResponse> patchUpdateNote(
      @PathVariable Long id, @RequestBody @Valid UpdateUpdateNoteRequest request) {
    UpdateNote note = adminUpdateNoteService.update(id, request);
    return ApiResponse.ok(UpdateNoteResponse.from(note));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> deleteUpdateNote(@PathVariable Long id) {
    adminUpdateNoteService.delete(id);
    return ApiResponse.ok(null);
  }

  @GetMapping
  public ApiResponse<CursorPage<UpdateNoteResponse, UpdateNoteCursor>> getUpdateNotes(
      @ModelAttribute @Valid UpdateNoteCursorRequest request) {
    CursorPage<UpdateNoteResponse, UpdateNoteCursor> result =
        adminUpdateNoteService.findList(request.toCursor(), request.sizeOrDefault());
    return ApiResponse.ok(result);
  }

  @GetMapping("/{id}")
  public ApiResponse<UpdateNoteResponse> getUpdateNoteById(@PathVariable Long id) {
    return ApiResponse.ok(adminUpdateNoteService.findOne(id));
  }
}
