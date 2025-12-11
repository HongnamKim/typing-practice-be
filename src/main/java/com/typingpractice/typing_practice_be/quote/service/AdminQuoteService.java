package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminQuoteService {
  private final QuoteRepository quoteRepository;
}
