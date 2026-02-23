package com.typingpractice.typing_practice_be.typingrecord.repository;

import com.typingpractice.typing_practice_be.typingrecord.domain.TypingRecord;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.QuoteTypingAggregation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
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

  public List<QuoteTypingAggregation> aggregateByQuoteIds(List<Long> quoteIds) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("outlier").is(false).and("quoteId").in(quoteIds)),
            Aggregation.group("quoteId")
                .first("language")
                .as("language")
                .count()
                .as("attemptsCount")
                .avg("cpm")
                .as("avgCpm")
                .avg("accuracy")
                .as("avgAcc")
                .avg("resetCount")
                .as("avgResetCount"),
            Aggregation.project()
                .and("_id")
                .as("quoteId")
                .andInclude("language", "attemptsCount", "avgCpm", "avgAcc", "avgResetCount"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", QuoteTypingAggregation.class)
        .getMappedResults();
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
                .as("attemptsCount")
                .avg("cpm")
                .as("avgCpm")
                .avg("accuracy")
                .as("avgAcc")
                .avg("resetCount")
                .as("avgResetCount"),
            Aggregation.project()
                .and("_id")
                .as("quoteId")
                .andInclude("language", "attemptsCount", "avgCpm", "avgAcc", "avgResetCount"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", QuoteTypingAggregation.class)
        .getMappedResults();
  }
}
