package com.typingpractice.typing_practice_be.quote.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteStatusRequest;
import com.typingpractice.typing_practice_be.quote.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuoteController {

  private final QuoteService quoteService;

  @GetMapping("/quotes")
  public ApiResponse<List<QuoteResponse>> getQuotes() {
    List<Quote> quotes = quoteService.findAll();

    return ApiResponse.ok(quotes.stream().map(QuoteResponse::from).toList());
  }

  @GetMapping("/quotes/{quoteId}")
  public ApiResponse<QuoteResponse> findQuoteById(@PathVariable Long quoteId) {
    Quote quote = quoteService.findById(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  @PostMapping("/quotes")
  public ApiResponse<QuoteResponse> create(@RequestBody @Valid QuoteCreateRequest request) {
    Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    Quote quote = quoteService.create(memberId, request);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  @PatchMapping("/quotes/{quoteId}")
  public ApiResponse<QuoteResponse> updateStatus(
      @PathVariable Long quoteId, @RequestBody @Valid QuoteStatusRequest request) {
    Quote quote = quoteService.updateQuoteStatus(quoteId, request.getStatus());

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  @DeleteMapping("/quotes/{quoteId}")
  public ApiResponse<Void> deleteQuote(@PathVariable Long quoteId) {
    quoteService.deleteQuote(quoteId);

    return ApiResponse.ok(null);
  }
}
