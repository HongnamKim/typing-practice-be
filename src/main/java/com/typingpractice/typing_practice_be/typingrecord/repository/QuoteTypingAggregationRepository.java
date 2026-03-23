package com.typingpractice.typing_practice_be.typingrecord.repository;

import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.QuoteTypingAggregation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QuoteTypingAggregationRepository {
  private final MongoTemplate mongoTemplate;

  public Map<Long, Integer> countAllByQuoteIds(List<Long> quoteIds) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("quoteId").in(quoteIds)),
            Aggregation.group("quoteId").count().as("count"),
            Aggregation.project().and("_id").as("quoteId").andInclude("count"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", Document.class)
        .getMappedResults()
        .stream()
        .collect(
            Collectors.toMap(
                doc -> ((Number) doc.get("quoteId")).longValue(), doc -> doc.getInteger("count")));
  }

  public List<QuoteTypingAggregation> aggregateByQuoteIds(List<Long> quoteIds) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("outlier").is(false).and("quoteId").in(quoteIds)),
            Aggregation.group("quoteId")
                .first("language")
                .as("language")
                .count()
                .as("validAttemptsCount")
                .avg("cpm")
                .as("avgCpm")
                .avg("accuracy")
                .as("avgAcc")
                .avg("resetCount")
                .as("avgResetCount"),
            Aggregation.project()
                .and("_id")
                .as("quoteId")
                .andInclude("language", "validAttemptsCount", "avgCpm", "avgAcc", "avgResetCount"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", QuoteTypingAggregation.class)
        .getMappedResults();
  }

  public Map<Long, Integer> countAllByQuoteIdsBetween(
      List<Long> quoteIds, LocalDateTime from, LocalDateTime to) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("quoteId").in(quoteIds).and("completedAt").gte(from).lt(to)),
            Aggregation.group("quoteId").count().as("count"),
            Aggregation.project().and("_id").as("quoteId").andInclude("count"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", Document.class)
        .getMappedResults()
        .stream()
        .collect(
            Collectors.toMap(
                doc -> ((Number) doc.get("quoteId")).longValue(), doc -> doc.getInteger("count")));
  }

  public List<QuoteTypingAggregation> aggregateByQuoteIdsBetween(
      List<Long> quoteIds, LocalDateTime from, LocalDateTime to) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("outlier")
                    .is(false)
                    .and("quoteId")
                    .in(quoteIds)
                    .and("completedAt")
                    .gte(from)
                    .lt(to)),
            Aggregation.group("quoteId")
                .first("language")
                .as("language")
                .count()
                .as("validAttemptsCount")
                .avg("cpm")
                .as("avgCpm")
                .avg("accuracy")
                .as("avgAcc")
                .avg("resetCount")
                .as("avgResetCount"),
            Aggregation.project()
                .and("_id")
                .as("quoteId")
                .andInclude("language", "validAttemptsCount", "avgCpm", "avgAcc", "avgResetCount"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", QuoteTypingAggregation.class)
        .getMappedResults();
  }
}
