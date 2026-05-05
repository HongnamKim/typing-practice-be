package com.typingpractice.typing_practice_be.notice.announcement.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
	@GetMapping
	public ApiResponse<Void> getAnnouncements() {
		return ApiResponse.ok(null);
	}

	@GetMapping("/latest")
	public ApiResponse<Void> getLatestAnnouncement() {
		return ApiResponse.ok(null);
	}

	@GetMapping("/pinned")
	public ApiResponse<Void> getPinnedAnnouncement() {
		return ApiResponse.ok(null);
	}

	@GetMapping("/{id}")
	public ApiResponse<Void> getAnnouncementById(@PathVariable Long id) {
		return ApiResponse.ok(null);
	}
}
