package com.typingpractice.typing_practice_be.quote.reject.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuoteSimilarityRejectLog extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long memberId;

  @Column(length = 100)
  private String inputSentence;

  @Column(length = 100)
  private String similarSentence;

  private float similarity;

  @Enumerated(EnumType.STRING)
  private QuoteLanguage language;

  public static QuoteSimilarityRejectLog create(
      Long memberId,
      String inputSentence,
      String similarSentence,
      float similarity,
      QuoteLanguage language) {
    QuoteSimilarityRejectLog log = new QuoteSimilarityRejectLog();
    log.memberId = memberId;
    log.inputSentence = inputSentence;
    log.similarSentence = similarSentence;
    log.similarity = similarity;
    log.language = language;

    return log;
  }
}
