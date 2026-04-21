package com.typingpractice.typing_practice_be.wordtypingrecord.repository;

import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypingRecord;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WordTypingRecordRepository {
  private final MongoTemplate mongoTemplate;

  public WordTypingRecord save(WordTypingRecord record) {
    return mongoTemplate.save(record);
  }

  public List<Long> findDistinctMemberIds() {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("memberId").ne(null)),
            Aggregation.group("memberId"),
            Aggregation.project().and("_id").as("memberId"));

    return mongoTemplate
        .aggregate(aggregation, "wordTypingRecord", Document.class)
        .getMappedResults()
        .stream()
        .map(doc -> ((Number) doc.get("memberId")).longValue())
        .toList();
  }

  public List<Long> findDistinctMemberIdsBetween(LocalDateTime from, LocalDateTime to) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("memberId").ne(null).and("completedAt").gte(from).lt(to)),
            Aggregation.group("memberId"),
            Aggregation.project().and("_id").as("memberId"));

    return mongoTemplate
        .aggregate(aggregation, "wordTypingRecord", Document.class)
        .getMappedResults()
        .stream()
        .map(doc -> ((Number) doc.get("memberId")).longValue())
        .toList();
  }
}
