package com.typingpractice.typing_practice_be.notice.announcement.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notice/announcement")
public class AnnouncementController {
  @GetMapping("")
  public ApiResponse<Void> getAnnouncements() {
    return ApiResponse.ok(null);
  }

  @GetMapping("/recent")
  public ApiResponse<Void> getRecentAnnouncement() {
    return ApiResponse.ok(null);
  }
}
