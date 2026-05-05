package com.typingpractice.typing_practice_be.notice.announcement.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {
  @PostMapping
  public ApiResponse<Void> postAnnouncement() {
    return ApiResponse.ok(null);
  }

  @PatchMapping("/{id}")
  public ApiResponse<Void> patchAnnouncement(@PathVariable Long id) {
    return ApiResponse.ok(null);
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
    return ApiResponse.ok(null);
  }

  @GetMapping
  public ApiResponse<Void> getAnnouncements() {
    return ApiResponse.ok(null);
  }

  @GetMapping("/pinned")
  public ApiResponse<Void> getPinnedAnnouncements() {
    return ApiResponse.ok(null);
  }

  @GetMapping("/{id}")
  public ApiResponse<Void> getAnnouncementById(@PathVariable Long id) {
    return ApiResponse.ok(null);
  }
}
