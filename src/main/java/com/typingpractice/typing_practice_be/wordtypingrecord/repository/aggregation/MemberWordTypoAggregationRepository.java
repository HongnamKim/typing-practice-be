package com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation;

import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypoAggregation;
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
public class MemberWordTypoAggregationRepository {
  private final MongoTemplate mongoTemplate;

  public List<MemberWordTypoAggregation> aggregateByMemberIdsBetween(
      List<Long> memberIds, LocalDateTime from, LocalDateTime to) {
    Aggregation aggregation =
        buildAggregation(
            Criteria.where("memberId")
                .in(memberIds)
                .and("completedAt")
                .gte(from)
                .lt(to)
                .and("outlier")
                .is(false));

    return mongoTemplate
        .aggregate(aggregation, "wordTypingRecord", MemberWordTypoAggregation.class)
        .getMappedResults();
  }

  public List<MemberWordTypoAggregation> aggregateByMemberIds(List<Long> memberIds) {
    Aggregation aggregation =
        buildAggregation(Criteria.where("memberId").in(memberIds).and("outlier").is(false));

    return mongoTemplate
        .aggregate(aggregation, "wordTypingRecord", MemberWordTypoAggregation.class)
        .getMappedResults();
  }

  private Aggregation buildAggregation(Criteria matchCriteria) {
    return Aggregation.newAggregation(
        Aggregation.match(matchCriteria),
        Aggregation.unwind("wordDetails"),
        Aggregation.unwind("wordDetails.typos"),
        Aggregation.group(
                Fields.fields("memberId")
                    .and("language")
                    .and("expected", "wordDetails.typos.expected")
                    .and("actual", "wordDetails.typos.actual"))
            .count()
            .as("count")
            .sum(
                ConditionalOperators.when(Criteria.where("wordDetails.typos.type").is("INITIAL"))
                    .then(1)
                    .otherwise(0))
            .as("initialCount")
            .sum(
                ConditionalOperators.when(Criteria.where("wordDetails.typos.type").is("MEDIAL"))
                    .then(1)
                    .otherwise(0))
            .as("medialCount")
            .sum(
                ConditionalOperators.when(Criteria.where("wordDetails.typos.type").is("FINAL"))
                    .then(1)
                    .otherwise(0))
            .as("finalCount")
            .sum(
                ConditionalOperators.when(Criteria.where("wordDetails.typos.type").is("LETTER"))
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
  }
}
