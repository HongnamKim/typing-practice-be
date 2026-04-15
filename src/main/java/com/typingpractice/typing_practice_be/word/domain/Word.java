package com.typingpractice.typing_practice_be.word.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE word SET deleted = true, deleted_at = NOW() where word_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_word",
            columnNames = {"word", "language"}))
public class Word extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "word_id")
  private Long id;

  private String word;

  @Enumerated(EnumType.STRING)
  private WordLanguage language;

  private Float difficulty;

  @Embedded private WordProfile profile;

  public static Word create(String word, WordLanguage language) {
    Word w = new Word();
    w.word = word;
    w.language = language;

    return w;
  }

  public void updateWord(String word) {
    this.word = word;
  }

  public void updateDifficulty(Float difficulty) {
    this.difficulty = difficulty;
  }

  public void updateProfile(WordProfile profile) {
    this.profile = profile;
  }
}
