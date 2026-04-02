package com.typingpractice.typing_practice_be.typingrecord.service;

import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.typingrecord.domain.TypingRecord;
import com.typingpractice.typing_practice_be.typingrecord.event.TypingRecordSavedEvent;
import com.typingpractice.typing_practice_be.typingrecord.query.TypingRecordQuery;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TypingRecordService {
  private final TypingRecordRepository typingRecordRepository;
  private final QuoteRepository quoteRepository;

  private final ApplicationEventPublisher eventPublisher;

  public TypingRecord save(Long memberId, TypingRecordQuery query) {
    Quote quote =
        quoteRepository.findById(query.getQuoteId()).orElseThrow(QuoteNotFoundException::new);

    TypingRecord record =
        TypingRecord.create(
            memberId,
            query.getAnonymousId(),
            quote.getId(),
            quote.getLanguage(),
            quote.getType(),
            query.getCpm(),
            query.getAccuracy(),
            query.getCharLength(),
            query.getResetCount(),
            query.getTypos(),
            query.getTracking());

    TypingRecord saved = typingRecordRepository.save(record);

    if (saved.isLoggedIn()) {
      eventPublisher.publishEvent(TypingRecordSavedEvent.from(saved));
    }

    return saved;
  }
}
