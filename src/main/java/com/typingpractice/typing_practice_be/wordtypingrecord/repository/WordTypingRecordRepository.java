package com.typingpractice.typing_practice_be.wordtypingrecord.repository;

import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypingRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WordTypingRecordRepository {
  private final MongoTemplate mongoTemplate;

  public WordTypingRecord save(WordTypingRecord record) {
    return mongoTemplate.save(record);
  }
}
