package com.typingpractice.typing_practice_be.typingrecord.repository;

import com.typingpractice.typing_practice_be.typingrecord.domain.TypingRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TypingRecordRepository {
  private final MongoTemplate mongoTemplate;

  public TypingRecord save(TypingRecord record) {
    return mongoTemplate.save(record);
  }
}
