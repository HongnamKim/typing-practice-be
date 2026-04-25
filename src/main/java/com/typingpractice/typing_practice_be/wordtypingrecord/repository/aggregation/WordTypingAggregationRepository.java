package com.typingpractice.typing_practice_be.wordtypingrecord.repository.aggregation;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.GlobalWordTypingPerformance;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.dto.aggregation.WordTypingAggregation;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class WordTypingAggregationRepository {
  private final MongoTemplate mongoTemplate;

  public GlobalWordTypingPerformance aggregateGlobalAvgByLanguage(WordLanguage language) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("language").is(language.name()).and("outlier").is(false)),
            Aggregation.group().avg("wpm").as("avgWpm").avg("accuracy").as("avgAcc"));

    List<Document> results =
        mongoTemplate.aggregate(aggregation, "wordTypingRecord", Document.class).getMappedResults();

    if (results.isEmpty()) {
      return GlobalWordTypingPerformance.empty();
    }

    Document doc = results.getFirst();
    Number wpmNum = (Number) doc.get("avgWpm");
    Number accNum = (Number) doc.get("avgAcc");

    if (wpmNum == null || accNum == null) {
      return GlobalWordTypingPerformance.empty();
    }

    return GlobalWordTypingPerformance.of(wpmNum.floatValue(), accNum.floatValue());
  }

  // outlier 제외, wordDetails unwind 후 wordId 별 집계
  public List<WordTypingAggregation> aggregateByWordIds(List<Long> wordIds) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("outlier").is(false)),
            Aggregation.unwind("wordDetails"),
            Aggregation.match(Criteria.where("wordDetails.wordId").in(wordIds)),
            Aggregation.group("wordDetails.wordId", "language")
                .count()
                .as("validAttemptsCount")
                .avg("wordDetails.timeMs")
                .as("avgTimeMs")
                .avg(cond("wordDetails.correct"))
                .as("correctRate"),
            Aggregation.project()
                .and("_id.wordId")
                .as("wordId")
                .and("_id.language")
                .as("language")
                .andInclude("validAttemptsCount", "avgTimeMs", "correctRate"));

    return mongoTemplate
        .aggregate(aggregation, "wordTypingRecord", WordTypingAggregation.class)
        .getMappedResults();
  }

  public List<WordTypingAggregation> aggregateByWordIdsBetween(
      List<Long> wordIds, LocalDateTime from, LocalDateTime to) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("outlier").is(false).and("completedAt").gte(from).lt(to)),
            Aggregation.unwind("wordDetails"),
            Aggregation.match(Criteria.where("wordDetails.wordId").in(wordIds)),
            Aggregation.group("wordDetails.wordId", "language")
                .count()
                .as("validAttemptsCount")
                .avg("wordDetails.timeMs")
                .as("avgTimeMs")
                .avg(cond("wordDetails.correct"))
                .as("correctRate"),
            Aggregation.project()
                .and("_id.wordId")
                .as("wordId")
                .and("_id.language")
                .as("language")
                .andInclude("validAttemptsCount", "avgTimeMs", "correctRate"));

    return mongoTemplate
        .aggregate(aggregation, "wordTypingRecord", WordTypingAggregation.class)
        .getMappedResults();
  }

  public Map<Long, Integer> countAllByWordIds(List<Long> wordIds) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.unwind("wordDetails"),
            Aggregation.match(Criteria.where("wordDetails.wordId").in(wordIds)),
            Aggregation.group("wordDetails.wordId").count().as("totalCount"));

    return toCountMap(
        mongoTemplate
            .aggregate(aggregation, "wordTypingRecord", Document.class)
            .getMappedResults());
  }

  public Map<Long, Integer> countAllByWordIdsBetween(
      List<Long> wordIds, LocalDateTime from, LocalDateTime to) {
    Aggregation aggregation =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("completedAt").gte(from).lt(to)),
            Aggregation.unwind("wordDetails"),
            Aggregation.match(Criteria.where("wordDetails.wordId").in(wordIds)),
            Aggregation.group("wordDetails.wordId").count().as("totalCount"));

    return toCountMap(
        mongoTemplate
            .aggregate(aggregation, "wordTypingRecord", Document.class)
            .getMappedResults());
  }

  // correct(boolean) → 1/0 로 변환하는 $cond 표현식
  private AggregationExpression cond(String field) {
    return ConditionalOperators.when(Criteria.where(field).is(true)).then(1).otherwise(0);
  }

  private Map<Long, Integer> toCountMap(List<Document> results) {
    Map<Long, Integer> map = new HashMap<>();
    for (Document doc : results) {
      Long wordId = ((Number) doc.get("_id")).longValue();
      Integer count = ((Number) doc.get("totalCount")).intValue();
      map.put(wordId, count);
    }

    return map;
  }
}
