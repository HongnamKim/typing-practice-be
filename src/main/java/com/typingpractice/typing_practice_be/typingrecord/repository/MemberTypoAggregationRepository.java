package com.typingpractice.typing_practice_be.typingrecord.repository;

import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypoAggregation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberTypoAggregationRepository {
  private final MongoTemplate mongoTemplate;

  public List<MemberTypoAggregation> aggregateByMemberIdsBetween(
      List<Long> memberIds, LocalDateTime from, LocalDateTime to) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("memberId")
                    .in(memberIds)
                    .and("completedAt")
                    .gte(from)
                    .lt(to)
                    .and("outlier")
                    .is(false)),
            Aggregation.unwind("typos"),
            Aggregation.group(
                    Fields.fields("memberId")
                        .and("language")
                        .and("expected", "typos.expected")
                        .and("actual", "typos.actual"))
                .count()
                .as("count")
                .sum(
                    ConditionalOperators.when(Criteria.where("typos.type").is("INITIAL"))
                        .then(1)
                        .otherwise(0))
                .as("initialCount")
                .sum(
                    ConditionalOperators.when(Criteria.where("typos.type").is("MEDIAL"))
                        .then(1)
                        .otherwise(0))
                .as("medialCount")
                .sum(
                    ConditionalOperators.when(Criteria.where("typos.type").is("FINAL"))
                        .then(1)
                        .otherwise(0))
                .as("finalCount")
                .sum(
                    ConditionalOperators.when(Criteria.where("typos.type").is("LETTER"))
                        .then(1)
                        .otherwise(0))
                .as("letterCount"),
            Aggregation.project()
                .and("_id.memberId")
                .as("memberId")
                .and("_id.language")
                .as("language")
                .and("_id.expected")
                .as("expected")
                .and("_id.actual")
                .as("actual")
                .andInclude("count", "initialCount", "medialCount", "finalCount", "letterCount"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", MemberTypoAggregation.class)
        .getMappedResults();
  }

  public List<MemberTypoAggregation> aggregateByMemberIds(List<Long> memberIds) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("memberId").in(memberIds).and("outlier").is(false)),
            Aggregation.unwind("typos"),
            Aggregation.group(
                    Fields.fields("memberId")
                        .and("language")
                        .and("expected", "typos.expected")
                        .and("actual", "typos.actual"))
                .count()
                .as("count")
                .sum(
                    ConditionalOperators.when(Criteria.where("typos.type").is("INITIAL"))
                        .then(1)
                        .otherwise(0))
                .as("initialCount")
                .sum(
                    ConditionalOperators.when(Criteria.where("typos.type").is("MEDIAL"))
                        .then(1)
                        .otherwise(0))
                .as("medialCount")
                .sum(
                    ConditionalOperators.when(Criteria.where("typos.type").is("FINAL"))
                        .then(1)
                        .otherwise(0))
                .as("finalCount")
                .sum(
                    ConditionalOperators.when(Criteria.where("typos.type").is("LETTER"))
                        .then(1)
                        .otherwise(0))
                .as("letterCount"),
            Aggregation.project()
                .and("_id.memberId")
                .as("memberId")
                .and("_id.language")
                .as("language")
                .and("_id.expected")
                .as("expected")
                .and("_id.actual")
                .as("actual")
                .andInclude("count", "initialCount", "medialCount", "finalCount", "letterCount"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", MemberTypoAggregation.class)
        .getMappedResults();
  }
}
