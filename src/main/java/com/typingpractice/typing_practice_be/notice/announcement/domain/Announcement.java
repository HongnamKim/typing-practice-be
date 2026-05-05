package com.typingpractice.typing_practice_be.notice.announcement.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@SQLRestriction("deleted = false")
@SQLDelete(
				sql = "UPDATE announcement SET deleted = true, deleted_at = NOW() WHERE announcement_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends BaseEntity {
	@Id
	@GeneratedValue
	@Column(name = "announcement_id")
	private Long id;

	@Column(columnDefinition = "timestamp with time zone", nullable = false)
	private LocalDateTime postedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private LocalizedText title;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private LocalizedText content;

	@Column(nullable = false)
	private boolean published = true;

	@Column(nullable = false)
	private boolean pinned;

	public static Announcement create(
					LocalDateTime postedAt,
					LocalizedText title,
					LocalizedText content,
					boolean published,
					boolean pinned) {
		Announcement a = new Announcement();
		a.postedAt = postedAt;
		a.title = title;
		a.content = content;
		a.published = published;
		a.pinned = pinned;
		return a;
	}

	public void update(
					LocalDateTime postedAt,
					LocalizedText title,
					LocalizedText content,
					Boolean published,
					Boolean pinned) {
		if (postedAt != null) this.postedAt = postedAt;
		if (title != null) this.title = title;
		if (content != null) this.content = content;
		if (published != null) this.published = published;
		if (pinned != null) this.pinned = pinned;
	}


}
