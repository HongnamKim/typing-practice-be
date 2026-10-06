package com.typingpractice.typing_practice_be.wordtypingrecord.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.CompoundIndex;

class WordTypingRecordIndexTest {

  @Test
  @DisplayName("wordTypingRecord 에 memberId + completedAt 복합 인덱스가 Atlas 와 같은 이름으로 선언되어 있다")
  void declaresMemberIdCompletedAtIndex() {
    CompoundIndex index = WordTypingRecord.class.getAnnotation(CompoundIndex.class);

    assertThat(index).isNotNull();
    assertThat(index.name()).isEqualTo("memberId_1_completedAt_1");
    assertThat(index.def().replaceAll("\\s", "")).isEqualTo("{'memberId':1,'completedAt':1}");
  }
}
