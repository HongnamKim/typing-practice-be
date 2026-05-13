package com.typingpractice.typing_practice_be.quote.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.dto.*;
import com.typingpractice.typing_practice_be.quote.query.QuotePaginationQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteUpdateQuery;
import com.typingpractice.typing_practice_be.quote.service.AdminQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
// @AdminOnly
@RequestMapping("/admin/quotes")
public class AdminQuoteController {
  private final AdminQuoteService adminQuoteService;

  @GetMapping()
  public ApiResponse<AdminQuotePaginationResponse> getQuotes(
      @ModelAttribute @Valid QuotePaginationRequest request) {

    QuotePaginationQuery query = QuotePaginationQuery.from(request);

    PageResult<Quote> result = adminQuoteService.findQuotes(query);

    return ApiResponse.ok(AdminQuotePaginationResponse.from(result));
  }

  @GetMapping("/{quoteId}")
  public ApiResponse<AdminQuoteResponse> getQuoteById(@PathVariable Long quoteId) {
    Quote quote = adminQuoteService.findQuoteByIdWithTypingStats(quoteId);

    return ApiResponse.ok(AdminQuoteResponse.from(quote));
  }

  // 승인
  @PostMapping("/{quoteId}/approve")
  public ApiResponse<QuoteResponse> approvePendingQuote(@PathVariable Long quoteId) {

    Quote quote = adminQuoteService.approvePublish(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 거부
  @PostMapping("/{quoteId}/reject")
  public ApiResponse<QuoteResponse> rejectPendingQuote(@PathVariable Long quoteId) {

    Quote quote = adminQuoteService.rejectPublish(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 공개 문장 수정
  @PatchMapping("/{quoteId}")
  public ApiResponse<QuoteResponse> patchQuote(
      @PathVariable Long quoteId, @RequestBody QuoteUpdateRequest request) {

    QuoteUpdateQuery query = QuoteUpdateQuery.from(request);

    Quote quote = adminQuoteService.updateQuote(quoteId, query);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 삭제
  @DeleteMapping("/{quoteId}")
  public ApiResponse<Void> deleteQuote(@PathVariable Long quoteId) {

    adminQuoteService.deleteQuote(quoteId);

    return ApiResponse.ok(null);
  }

  @PatchMapping("/{quoteId}/hide")
  public ApiResponse<QuoteResponse> hideQuote(@PathVariable Long quoteId) {
    Quote hideQuote = adminQuoteService.hideQuote(quoteId);

    return ApiResponse.ok(QuoteResponse.from(hideQuote));
  }

  // 숨김 해제
  @PostMapping("/{quoteId}/restore")
  public ApiResponse<QuoteResponse> restoreHiddenQuotes(@PathVariable Long quoteId) {

    Quote quote = adminQuoteService.cancelHidden(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  @GetMapping("/deleted")
  public ApiResponse<DeletedQuotePaginationResponse> getDeletedQuotes(
      @ModelAttribute @Valid QuotePaginationRequest request) {
    QuotePaginationQuery query = QuotePaginationQuery.from(request);

    PageResult<Quote> result = adminQuoteService.findDeletedQuotes(query);

    return ApiResponse.ok(DeletedQuotePaginationResponse.from(result));
  }

  @DeleteMapping("/{quoteId}/permanent")
  public ApiResponse<Void> permanentDeleteQuote(@PathVariable Long quoteId) {
    adminQuoteService.permanentDeleteQuote(quoteId);

    return ApiResponse.ok(null);
  }
}
