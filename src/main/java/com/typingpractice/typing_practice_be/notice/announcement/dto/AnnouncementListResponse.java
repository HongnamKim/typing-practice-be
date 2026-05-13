package com.typingpractice.typing_practice_be.notice.announcement.dto;

import java.util.List;

public record AnnouncementListResponse(List<AnnouncementSummary> items) {}
