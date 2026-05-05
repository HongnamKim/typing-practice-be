package com.typingpractice.typing_practice_be.notice.announcement.dto;

import com.typingpractice.typing_practice_be.notice.announcement.domain.Announcement;
import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;

import java.time.LocalDateTime;

public record AnnouncementDetail(
				Long id,
				LocalDateTime postedAt,
				LocalizedText title,
				LocalizedText content,
				boolean published,
				boolean pinned,
				LocalDateTime createdAt,
				LocalDateTime updatedAt) {

	public static AnnouncementDetail from(Announcement a) {
		return new AnnouncementDetail(
						a.getId(),
						a.getPostedAt(),
						a.getTitle(),
						a.getContent(),
						a.isPublished(),
						a.isPinned(),
						a.getCreatedAt(),
						a.getUpdatedAt());
	}
}
