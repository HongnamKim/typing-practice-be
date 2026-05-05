package com.typingpractice.typing_practice_be.notice.announcement.dto;

import com.typingpractice.typing_practice_be.notice.announcement.domain.Announcement;
import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;

import java.time.LocalDateTime;

public record AnnouncementSummary(
				Long id,
				LocalDateTime postedAt,
				LocalizedText title,
				boolean published,
				boolean pinned,
				LocalDateTime createdAt,
				LocalDateTime updatedAt) {

	public static AnnouncementSummary from(Announcement a) {
		return new AnnouncementSummary(
						a.getId(),
						a.getPostedAt(),
						a.getTitle(),
						a.isPublished(),
						a.isPinned(),
						a.getCreatedAt(),
						a.getUpdatedAt());
	}
}
