package com.typingpractice.typing_practice_be.quote.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.dto.*;
import com.typingpractice.typing_practice_be.quote.exception.EmptyUpdateRequestException;
import com.typingpractice.typing_practice_be.quote.query.PublicQuoteQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteCreateQuery;
import com.typingpractice.typing_practice_be.quote.query.QuotePaginationQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteUpdateQuery;
import com.typingpractice.typing_practice_be.quote.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QuoteController {

  private final QuoteService quoteService;

  private Long getMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }

  // 공개 문장 랜덤 조회
  @GetMapping("/quotes")
  public ApiResponse<QuotePaginationResponse> getPublicQuotes(
      @ModelAttribute @Valid PublicQuoteRequest request) {

    Long memberId = request.getOnlyMyQuotes() ? getMemberId() : null;

    PublicQuoteQuery query = PublicQuoteQuery.from(memberId, request);

    PageResult<Quote> result = quoteService.findRandomPublicQuotes(query);

    return ApiResponse.ok(QuotePaginationResponse.from(result));
  }

  // 내 문장 조회
  @GetMapping("/quotes/my")
  public ApiResponse<QuotePaginationResponse> getMyQuotes(
      @ModelAttribute @Valid QuotePaginationRequest request) {
    Long memberId = getMemberId();

    QuotePaginationQuery query = QuotePaginationQuery.from(request);

    PageResult<Quote> result = quoteService.getMyQuotes(memberId, query);

    return ApiResponse.ok(QuotePaginationResponse.from(result));
  }

  // 상세 조회
  @GetMapping("/quotes/{quoteId}")
  public ApiResponse<QuoteResponse> findQuoteById(@PathVariable Long quoteId) {
    Quote quote = quoteService.findById(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 공개 문장 업로드
  // @BannedNotAllowed
  @PostMapping("/quotes/public")
  public ApiResponse<QuoteResponse> createPublicQuote(
      @RequestBody @Valid QuoteCreateRequest request) {
    Long memberId = getMemberId();

    QuoteCreateQuery query = QuoteCreateQuery.ofPublic(request);

    Quote quote = quoteService.create(memberId, query);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 비공개 문장 업로드
  @PostMapping("/quotes/private")
  public ApiResponse<QuoteResponse> createPrivateQuote(
      @RequestBody @Valid QuoteCreateRequest request) {
    Long memberId = getMemberId();

    QuoteCreateQuery query = QuoteCreateQuery.ofPrivate(request);

    Quote quote = quoteService.create(memberId, query);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 비공개 문장 수정
  @PatchMapping("/quotes/{quoteId}")
  public ApiResponse<QuoteResponse> updatePrivateQuote(
      @PathVariable Long quoteId, @RequestBody @Valid QuoteUpdateRequest request) {
    if (request.getSentence() == null && request.getAuthor() == null) {
      throw new EmptyUpdateRequestException();
    }

    Long memberId = getMemberId();
    QuoteUpdateQuery query = QuoteUpdateQuery.from(request);

    Quote quote = quoteService.updatePrivateQuote(memberId, quoteId, query);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 비공개 문장 삭제
  @DeleteMapping("/quotes/{quoteId}")
  public ApiResponse<Void> deleteQuote(@PathVariable Long quoteId) {
    Long memberId = getMemberId();

    quoteService.deleteQuote(memberId, quoteId);

    return ApiResponse.ok(null);
  }

  // 개인용 문장 공개 전환
  // @BannedNotAllowed
  @PostMapping("/quotes/{quoteId}/publish")
  public ApiResponse<QuoteResponse> publishQuote(@PathVariable Long quoteId) {
    Long memberId = getMemberId();

    Quote quote = quoteService.publishQuote(memberId, quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 공개 전환 취소
  @PostMapping("/quotes/{quoteId}/cancel-publish")
  public ApiResponse<QuoteResponse> cancelPublishQuote(@PathVariable Long quoteId) {
    Long memberId = getMemberId();

    Quote quote = quoteService.cancelPublishQuote(memberId, quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }
}
