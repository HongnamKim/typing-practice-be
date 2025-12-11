package com.typingpractice.typing_practice_be.quote.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
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

  private Long getMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }

  // 문장 업로드
  @PostMapping("/quotes")
  public ApiResponse<QuoteResponse> create(@RequestBody @Valid QuoteCreateRequest request) {
    Long memberId = getMemberId();

    Quote quote = quoteService.create(memberId, request);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 공개 문장 조회
  @GetMapping("/quotes")
  public ApiResponse<List<QuoteResponse>> getPublicQuotes() {
    List<Quote> quotes = quoteService.findPublicQuotes();

    return ApiResponse.ok(quotes.stream().map(QuoteResponse::from).toList());
  }

  // 내 문장 조회
  @GetMapping("/quotes/my")
  public ApiResponse<List<QuoteResponse>> getMyQuotes() {
    Long memberId = getMemberId();

    List<Quote> myQuotes = quoteService.getMyQuotes(memberId);

    return ApiResponse.ok(myQuotes.stream().map(QuoteResponse::from).toList());
  }

  // 상세 조회
  @GetMapping("/quotes/{quoteId}")
  public ApiResponse<QuoteResponse> findQuoteById(@PathVariable Long quoteId) {
    Quote quote = quoteService.findById(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 비공개 문장 수정
  @PatchMapping("/quotes/{quoteId}")
  public ApiResponse<QuoteResponse> updatePrivateQuote(
      @PathVariable Long quoteId, @RequestBody @Valid QuoteUpdateRequest request) {
    if (request.getSentence() == null && request.getAuthor() == null) {
      throw new IllegalArgumentException("수정할 내용이 없습니다.");
    }

    Long memberId = getMemberId();
    Quote quote = quoteService.updatePrivateQuote(memberId, quoteId, request);

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
