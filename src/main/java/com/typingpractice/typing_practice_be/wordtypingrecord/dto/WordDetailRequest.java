package com.typingpractice.typing_practice_be.wordtypingrecord.dto;

import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordDetail;
import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class WordDetailRequest {
  @NotNull private Integer wordIndex;
  @NotNull private Long wordId;
  @NotNull private String word;
  private String typed;
  private boolean correct;
  private long timeMs;

  @Valid private List<WordTypoRequest> typos;

  public WordDetail toWordDetail() {
    List<WordTypo> wordTypos =
        (typos == null || typos.isEmpty())
            ? List.of()
            : typos.stream().map(WordTypoRequest::toWordTypo).toList();

    return WordDetail.create(
        wordIndex, wordId, word, typed != null ? typed : "", correct, timeMs, wordTypos);
  }
}
