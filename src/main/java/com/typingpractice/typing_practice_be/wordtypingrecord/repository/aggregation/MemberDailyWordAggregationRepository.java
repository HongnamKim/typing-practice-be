package com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberDailyWordAggregation;
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
public class MemberDailyWordAggregationRepository {
  private final MongoTemplate mongoTemplate;

  public List<MemberDailyWordAggregation> aggregateByMemberIdsBetween(
      List<Long> memberIds, LocalDateTime from, LocalDateTime to) {
    return run(
        buildAggregation(
            Criteria.where("memberId")
                .in(memberIds)
                .and("completedAt")
                .gte(from)
                .lt(to)
                .and("outlier")
                .is(false)));
  }

  public List<MemberDailyWordAggregation> aggregateByMemberIdsAndLanguageBetween(
      List<Long> memberIds, WordLanguage language, LocalDateTime from, LocalDateTime to) {
    return run(
        buildAggregation(
            Criteria.where("memberId")
                .in(memberIds)
                .and("language")
                .is(language.name())
                .and("completedAt")
                .gte(from)
                .lt(to)
                .and("outlier")
                .is(false)));
  }

  private Aggregation buildAggregation(Criteria matchCriteria) {
    return Aggregation.newAggregation(
        Aggregation.match(matchCriteria),
        Aggregation.addFields()
            .addFieldWithValue(
                "date",
                DateOperators.dateOf("completedAt")
                    .withTimezone(DateOperators.Timezone.valueOf(TimeUtils.KST_OFFSET))
                    .toString("%Y-%m-%d"))
            .build(),
        Aggregation.group(Fields.fields("memberId").and("date").and("language"))
            .count()
            .as("attempts")
            .sum("wordCount")
            .as("wordsAttempted")
            .avg("wpm")
            .as("avgWpm")
            .avg("accuracy")
            .as("avgAcc")
            .max("wpm")
            .as("bestWpm")
            .sum(ArithmeticOperators.Divide.valueOf("elapsedTimeMs").divideBy(60000))
            .as("practiceTimeMin"),
        Aggregation.project()
            .and("_id.memberId")
            .as("memberId")
            .and("_id.date")
            .as("date")
            .and("_id.language")
            .as("language")
            .andInclude(
                "attempts", "wordsAttempted", "avgWpm", "avgAcc", "bestWpm", "practiceTimeMin"));
  }

  private List<MemberDailyWordAggregation> run(Aggregation aggregation) {
    return mongoTemplate
        .aggregate(aggregation, "wordTypingRecord", MemberDailyWordAggregation.class)
        .getMappedResults();
  }
}
