package com.typingpractice.typing_practice_be.quote.reject.service;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.reject.domain.QuoteSimilarityRejectLog;
import com.typingpractice.typing_practice_be.quote.reject.repository.QuoteSimilarityRejectLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteSimilarityRejectService {
  private final QuoteSimilarityRejectLogRepository rejectLogRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(
      Long memberId,
      String inputSentence,
      String similarSentence,
      float similarity,
      QuoteLanguage language) {
    QuoteSimilarityRejectLog log =
        QuoteSimilarityRejectLog.create(
            memberId, inputSentence, similarSentence, similarity, language);

    rejectLogRepository.save(log);
  }
}
