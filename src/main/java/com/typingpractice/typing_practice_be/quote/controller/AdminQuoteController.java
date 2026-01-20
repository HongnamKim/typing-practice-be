package com.typingpractice.typing_practice_be.quote.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.common.security.admin.AdminOnly;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.dto.QuotePaginationRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuotePaginationResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
import com.typingpractice.typing_practice_be.quote.query.QuotePaginationQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteUpdateQuery;
import com.typingpractice.typing_practice_be.quote.service.AdminQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@AdminOnly
public class AdminQuoteController {
  private final AdminQuoteService adminQuoteService;

  @GetMapping("/admin/quotes")
  public ApiResponse<QuotePaginationResponse> getQuotes(
      @ModelAttribute @Valid QuotePaginationRequest request) {

    QuotePaginationQuery query = QuotePaginationQuery.from(request);

    PageResult<Quote> result = adminQuoteService.findQuotes(query);

    return ApiResponse.ok(QuotePaginationResponse.from(result));
  }

  // 승인
  @PostMapping("/admin/quotes/{quoteId}/approve")
  public ApiResponse<QuoteResponse> approvePendingQuote(@PathVariable Long quoteId) {

    Quote quote = adminQuoteService.approvePublish(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 거부
  @PostMapping("/admin/quotes/{quoteId}/reject")
  public ApiResponse<QuoteResponse> rejectPendingQuote(@PathVariable Long quoteId) {

    Quote quote = adminQuoteService.rejectPublish(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 공개 문장 수정
  @PatchMapping("/admin/quotes/{quoteId}")
  public ApiResponse<QuoteResponse> patchQuote(
      @PathVariable Long quoteId, @RequestBody QuoteUpdateRequest request) {

    QuoteUpdateQuery query = QuoteUpdateQuery.from(request);

    Quote quote = adminQuoteService.updateQuote(quoteId, query);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 삭제
  @DeleteMapping("/admin/quotes/{quoteId}")
  public ApiResponse<Void> deleteQuote(@PathVariable Long quoteId) {

    adminQuoteService.deleteQuote(quoteId);

    return ApiResponse.ok(null);
  }

  // 숨김 해제
  @PostMapping("/admin/quotes/{quoteId}/restore")
  public ApiResponse<QuoteResponse> restoreHiddenQuotes(@PathVariable Long quoteId) {

    Quote quote = adminQuoteService.cancelHidden(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }
}
