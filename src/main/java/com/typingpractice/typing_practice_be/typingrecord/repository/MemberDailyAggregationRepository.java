package com.typingpractice.typing_practice_be.typingrecord.repository;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.MemberDailyAggregation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberDailyAggregationRepository {
  private final MongoTemplate mongoTemplate;

  public List<MemberDailyAggregation> aggregateByMemberIdsBetween(
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
            Aggregation.addFields()
                .addFieldWithValue(
                    "date",
                    DateOperators.dateOf("completedAt")
                        .withTimezone(DateOperators.Timezone.valueOf(TimeUtils.KST_OFFSET))
                        .toString("%Y-%m-%d"))
                .build(),
            Aggregation.group(Fields.fields("memberId").and("date"))
                .count()
                .as("attempts")
                .avg("cpm")
                .as("avgCpm")
                .avg("accuracy")
                .as("avgAcc")
                .max("cpm")
                .as("bestCpm")
                .sum("resetCount")
                .as("resetCount")
                .sum(ArithmeticOperators.valueOf("charLength").divideBy("cpm"))
                .as("practiceTimeMin"),
            Aggregation.project()
                .and("_id.memberId")
                .as("memberId")
                .and("_id.date")
                .as("date")
                .andInclude(
                    "attempts", "avgCpm", "avgAcc", "bestCpm", "resetCount", "practiceTimeMin"));

    return mongoTemplate
        .aggregate(aggregation, "typingRecord", MemberDailyAggregation.class)
        .getMappedResults();
  }
}
