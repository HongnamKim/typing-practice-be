package com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.MemberWordTypingAggregation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberWordTypingAggregationRepository {
  private final MongoTemplate mongoTemplate;

  public List<MemberWordTypingAggregation> aggregateByMemberIds(List<Long> memberIds) {
    Aggregation aggregation =
        buildAggregation(Criteria.where("memberId").in(memberIds).and("outlier").is(false));
    return mongoTemplate
        .aggregate(aggregation, "wordTypingRecord", MemberWordTypingAggregation.class)
        .getMappedResults();
  }

  public List<MemberWordTypingAggregation> aggregateByMemberIdsBetween(
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
        .aggregate(aggregation, "wordTypingRecord", MemberWordTypingAggregation.class)
        .getMappedResults();
  }

  public List<MemberWordTypingAggregation> aggregateByMemberIdsAndLanguageBetween(
      List<Long> memberIds, WordLanguage language, LocalDateTime from, LocalDateTime to) {
    Aggregation aggregation =
        buildAggregation(
            Criteria.where("memberId")
                .in(memberIds)
                .and("language")
                .is(language.name())
                .and("completedAt")
                .gte(from)
                .lt(to)
                .and("outlier")
                .is(false));
    return mongoTemplate
        .aggregate(aggregation, "wordTypingRecord", MemberWordTypingAggregation.class)
        .getMappedResults();
  }

  private Aggregation buildAggregation(Criteria matchCriteria) {
    return Aggregation.newAggregation(
        Aggregation.match(matchCriteria),
        Aggregation.group("memberId", "language")
            .count()
            .as("totalAttempts")
            .sum("wordCount")
            .as("totalWordsAttempted")
            .avg("wpm")
            .as("avgWpm")
            .avg("accuracy")
            .as("avgAcc")
            .max("wpm")
            .as("bestWpm")
            .sum(ArithmeticOperators.Divide.valueOf("elapsedTimeMs").divideBy(60000))
            .as("totalPracticeTimeMin")
            .max("completedAt")
            .as("lastPracticedAt"),
        Aggregation.project()
            .and("_id.memberId")
            .as("memberId")
            .and("_id.language")
            .as("language")
            .andInclude(
                "totalAttempts",
                "totalWordsAttempted",
                "avgWpm",
                "avgAcc",
                "bestWpm",
                "totalPracticeTimeMin",
                "lastPracticedAt"));
  }
}
