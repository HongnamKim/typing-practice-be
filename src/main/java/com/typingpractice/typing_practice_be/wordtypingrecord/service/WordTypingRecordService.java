package com.typingpractice.typing_practice_be.wordtypingrecord.service;

import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypingRecord;
import com.typingpractice.typing_practice_be.wordtypingrecord.query.WordTypingRecordQuery;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.WordTypingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordTypingRecordService {
  private final WordTypingRecordRepository repository;
  private final WordTypingRecordFallbackService fallbackService;

  public WordTypingRecord save(Long memberId, WordTypingRecordQuery query) {
    WordTypingRecord record =
        WordTypingRecord.create(
            memberId,
            query.getAnonymousId(),
            query.getLanguage(),
            query.getDifficulty(),
            query.getWordCount(),
            query.getWpm(),
            query.getAccuracy(),
            query.getCorrectWordCount(),
            query.getIncorrectWordCount(),
            query.getElapsedTimeMs(),
            query.getWordIds(),
            query.getWordDetails());

    WordTypingRecord saved;
    try {
      saved = repository.save(record);
    } catch (Exception e) {
      log.warn("[WordTypingRecord] MongoDB 저장 실패 -> fallback: {}", e.getMessage());
      fallbackService.writeToFile(record);
      return record;
    }

    fallbackService.flushIfNeeded();

    // TODO: Step 5, 6에서 이벤트 발행 추가

    return saved;
  }
}
