package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQuoteService {
  private final QuoteRepository quoteRepository;
}
