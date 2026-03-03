package com.typingpractice.typing_practice_be.typingrecord.repository;

import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberTypingAggregation;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberTypingAggregationRepository {
  private final MongoTemplate mongoTemplate;

  public List<MemberTypingAggregation> aggregateByMemberIds(List<Long> memberIds) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("memberId").in(memberIds).and("cpm").gt(0)),
            Aggregation.group("memberId")
                .count()
                .as("totalAttempts")
                .avg("cpm")
                .as("avgCpm")
                .avg("accuracy")
                .as("avgAcc")
                .max("cpm")
                .as("bestCpm")
                .sum(ArithmeticOperators.valueOf("charLength").divideBy("cpm"))
                .as("totalPracticeTimeMin")
                .sum("resetCount")
                .as("totalResetCount")
                .max("completedAt")
                .as("lastPracticedAt"),
            Aggregation.project()
                .and("_id")
                .as("memberId")
                .andInclude(
                    "totalAttempts",
                    "avgCpm",
                    "avgAcc",
                    "bestCpm",
                    "totalPracticeTimeMin",
                    "totalResetCount",
                    "lastPracticedAt"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", MemberTypingAggregation.class)
        .getMappedResults();
  }

  public List<MemberTypingAggregation> aggregateByMemberIdsBetween(
      List<Long> memberIds, LocalDateTime from, LocalDateTime to) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("memberId")
                    .in(memberIds)
                    .and("completedAt")
                    .gte(from)
                    .lt(to)
                    .and("cpm")
                    .gt(0)),
            Aggregation.group("memberId")
                .count()
                .as("totalAttempts")
                .avg("cpm")
                .as("avgCpm")
                .avg("accuracy")
                .as("avgAcc")
                .max("cpm")
                .as("bestCpm")
                .sum(ArithmeticOperators.valueOf("charLength").divideBy("cpm"))
                .as("totalPracticeTimeMin")
                .sum("resetCount")
                .as("totalResetCount")
                .max("completedAt")
                .as("lastPracticedAt"),
            Aggregation.project()
                .and("_id")
                .as("memberId")
                .andInclude(
                    "totalAttempts",
                    "avgCpm",
                    "avgAcc",
                    "bestCpm",
                    "totalPracticeTimeMin",
                    "totalResetCount",
                    "lastPracticedAt"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", MemberTypingAggregation.class)
        .getMappedResults();
  }
}
