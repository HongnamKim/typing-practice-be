package com.typingpractice.typing_practice_be.notice.update.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.notice.domain.LocalizedText;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@Table(
    uniqueConstraints = @UniqueConstraint(name = "uq_update_note_version", columnNames = "version"))
@SQLRestriction("deleted = false")
@SQLDelete(
    sql = "UPDATE update_note SET deleted = true, deleted_at = NOW() WHERE update_note_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateNote extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "update_note_id")
  private Long id;

  @Column(nullable = false, length = 32)
  private String version;

  @Column(columnDefinition = "timestamp with time zone", nullable = false)
  private LocalDateTime releasedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private List<LocalizedText> newFeatures = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private List<LocalizedText> improvements = new ArrayList<>();

  @Column(nullable = false)
  private boolean published;

  public static UpdateNote create(
      String version,
      LocalDateTime releasedAt,
      List<LocalizedText> newFeatures,
      List<LocalizedText> improvements,
      boolean published) {
    UpdateNote note = new UpdateNote();
    note.version = version;
    note.releasedAt = releasedAt;
    note.newFeatures = (newFeatures != null) ? new ArrayList<>(newFeatures) : new ArrayList<>();
    note.improvements = (improvements != null) ? new ArrayList<>(improvements) : new ArrayList<>();
    note.published = published;
    return note;
  }

  /** PATCH 부분 수정. null = 미변경. */
  public void update(
      String version,
      LocalDateTime releasedAt,
      List<LocalizedText> newFeatures,
      List<LocalizedText> improvements,
      Boolean published) {
    if (version != null) this.version = version;
    if (releasedAt != null) this.releasedAt = releasedAt;
    if (newFeatures != null) this.newFeatures = new ArrayList<>(newFeatures);
    if (improvements != null) this.improvements = new ArrayList<>(improvements);
    if (published != null) this.published = published;
  }

  public List<LocalizedText> getNewFeatures() {
    return Collections.unmodifiableList(newFeatures);
  }

  public List<LocalizedText> getImprovements() {
    return Collections.unmodifiableList(improvements);
  }
}
