package com.typingpractice.typing_practice_be.notice.announcement.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.CursorPage;
import com.typingpractice.typing_practice_be.notice.announcement.dto.*;
import com.typingpractice.typing_practice_be.notice.announcement.service.AdminAnnouncementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {
  private final AdminAnnouncementService adminAnnouncementService;

  @PostMapping
  public ApiResponse<AnnouncementIdResponse> postAnnouncement(
      @RequestBody @Valid CreateAnnouncementRequest request) {
    Long id = adminAnnouncementService.create(request);
    return ApiResponse.ok(new AnnouncementIdResponse(id));
  }

  @PatchMapping("/{id}")
  public ApiResponse<Void> patchAnnouncement(
      @PathVariable Long id, @RequestBody @Valid UpdateAnnouncementRequest request) {
    adminAnnouncementService.update(id, request);
    return ApiResponse.ok(null);
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
    adminAnnouncementService.delete(id);
    return ApiResponse.ok(null);
  }

  @GetMapping
  public ApiResponse<CursorPage<AnnouncementSummary, AnnouncementCursor>> getAnnouncements(
      @ModelAttribute @Valid AnnouncementCursorRequest request) {
    CursorPage<AnnouncementSummary, AnnouncementCursor> result =
        adminAnnouncementService.findList(request.toCursor(), request.sizeOrDefault());

    return ApiResponse.ok(result);
  }

  @GetMapping("/pinned")
  public ApiResponse<AnnouncementListResponse> getPinnedAnnouncements() {

    List<AnnouncementSummary> items = adminAnnouncementService.findPinned();

    return ApiResponse.ok(new AnnouncementListResponse(items));
  }

  @GetMapping("/{id}")
  public ApiResponse<AnnouncementDetail> getAnnouncementById(@PathVariable Long id) {
    return ApiResponse.ok(adminAnnouncementService.findOne(id));
  }
}
