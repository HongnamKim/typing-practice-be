package com.typingpractice.typing_practice_be.wordtypingrecord.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordDetail {
  private int wordIndex;
  private Long wordId;
  private String word;
  private String typed;
  private boolean correct;
  private long timeMs;
  private List<WordTypo> typos;

  public static WordDetail create(
      int wordIndex,
      Long wordId,
      String word,
      String typed,
      boolean correct,
      long timeMs,
      List<WordTypo> typos) {
    WordDetail detail = new WordDetail();
    detail.wordIndex = wordIndex;
    detail.wordId = wordId;
    detail.word = word;
    detail.typed = typed;
    detail.correct = correct;
    detail.timeMs = timeMs;
    detail.typos = typos;
    return detail;
  }
}
