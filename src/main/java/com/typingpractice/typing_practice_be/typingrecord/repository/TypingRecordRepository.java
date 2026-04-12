package com.typingpractice.typing_practice_be.typingrecord.repository;

import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveServingRecord;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.domain.TypingRecord;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TypingRecordRepository {
  private final MongoTemplate mongoTemplate;

  public TypingRecord save(TypingRecord record) {
    return mongoTemplate.save(record);
  }

  public boolean existsByMemberIdBetween(Long memberId, LocalDateTime from, LocalDateTime to) {
    long count =
        mongoTemplate.count(
            Query.query(
                Criteria.where("memberId").is(memberId).and("completedAt").gte(from).lt(to)),
            "typingRecord");

    return count > 0;
  }

  public List<Long> findDistinctMemberIds() {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("memberId").ne(null)),
            Aggregation.group("memberId"),
            Aggregation.project().and("_id").as("memberId"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", Document.class)
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
        .aggregate(aggregation, "typingRecord", Document.class)
        .getMappedResults()
        .stream()
        .map(doc -> ((Number) doc.get("memberId")).longValue())
        .toList();
  }

  // MemberTypingStats 의 mu, sigma 증분 배치: 전날 기록
  public List<AdaptiveServingRecord> findForAdaptiveServingBetween(
      Long memberId, QuoteLanguage language, LocalDateTime from, LocalDateTime to) {
    Query query =
        Query.query(
                Criteria.where("memberId")
                    .is(memberId)
                    .and("language")
                    .is(language.name())
                    .and("completedAt")
                    .gte(from)
                    .lt(to)
                    .and("outlier")
                    .is(false)
                    .and("avgCpmSnapshot")
                    .gt(0)
                    .and("avgAccSnapshot")
                    .gt(0))
            .with(Sort.by(Sort.Direction.ASC, "completedAt"));

    query
        .fields()
        .include("cpm", "accuracy", "quoteDifficulty", "avgCpmSnapshot", "avgAccSnapshot");

    return mongoTemplate.find(query, AdaptiveServingRecord.class, "typingRecord");
  }

  // MemberTypingStats 의 mu, sigma 전체 재계산
  public List<AdaptiveServingRecord> findForAdaptiveServing(Long memberId, QuoteLanguage language) {

    Query query =
        Query.query(
                Criteria.where("memberId")
                    .is(memberId)
                    .and("language")
                    .is(language.name())
                    .and("outlier")
                    .is(false)
                    .and("avgCpmSnapshot")
                    .gt(0)
                    .and("avgAccSnapshot")
                    .gt(0))
            .with(Sort.by(Sort.Direction.ASC, "completedAt"));

    query
        .fields()
        .include("cpm", "accuracy", "quoteDifficulty", "avgCpmSnapshot", "avgAccSnapshot");

    return mongoTemplate.find(query, AdaptiveServingRecord.class, "typingRecord");
  }
}
