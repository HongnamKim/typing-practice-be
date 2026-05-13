package com.typingpractice.typing_practice_be.notice.announcement.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.announcement.dto.*;
import com.typingpractice.typing_practice_be.notice.announcement.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
  private final AnnouncementService announcementService;

  @GetMapping
  public ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>> getAnnouncements(
      @ModelAttribute @Valid AnnouncementCursorRequest request) {

    CursorPage<AnnouncementSummary, AnnouncementCursor> result =
        announcementService.findList(request.toCursor(), request.sizeOrDefault());

    return ApiResponse.ok(result);
  }

  @GetMapping("/latest")
  public ApiResponse<AnnouncementDetail> getLatestAnnouncement() {
    return ApiResponse.ok(announcementService.findLatest());
  }

  @GetMapping("/pinned")
  public ApiResponse<AnnouncementListResponse> getPinnedAnnouncement() {
    List<AnnouncementSummary> items = announcementService.findPinned();

    return ApiResponse.ok(new AnnouncementListResponse(items));
  }

  @GetMapping("/{id}")
  public ApiResponse<AnnouncementDetail> getAnnouncementById(@PathVariable Long id) {
    return ApiResponse.ok(announcementService.findOne(id));
  }
}
