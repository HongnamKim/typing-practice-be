package com.typingpractice.typing_practice_be.quote.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.exception.NotAdminException;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
import com.typingpractice.typing_practice_be.quote.service.AdminQuoteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdminQuoteController {
  private final MemberService memberService;
  private final AdminQuoteService adminQuoteService;

  private void validateAdmin() {
    Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Member member = memberService.findMemberById(memberId);

    if (member.getRole() != MemberRole.ADMIN) {
      throw new NotAdminException();
    }
  }

  // 승인 대기 목록 조회
  @GetMapping("/admin/quotes/pending")
  public ApiResponse<List<QuoteResponse>> getPendingQuotes() {
    validateAdmin();

    List<Quote> pendingQuotes = adminQuoteService.findPendingQuotes();

    return ApiResponse.ok(pendingQuotes.stream().map(QuoteResponse::from).toList());
  }

  // 승인
  @PostMapping("/admin/quotes/{quoteId}/approve")
  public ApiResponse<QuoteResponse> approvePendingQuote(@PathVariable Long quoteId) {
    validateAdmin();

    Quote quote = adminQuoteService.approvePublish(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 거부
  @PostMapping("/admin/quotes/{quoteId}/reject")
  public ApiResponse<QuoteResponse> rejectPendingQuote(@PathVariable Long quoteId) {
    validateAdmin();

    Quote quote = adminQuoteService.rejectPublish(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 공개 문장 수정
  @PatchMapping("/admin/quotes/{quoteId}")
  public ApiResponse<QuoteResponse> patchQuote(
      @PathVariable Long quoteId, @RequestBody QuoteUpdateRequest request) {
    validateAdmin();

    Quote quote = adminQuoteService.updateQuote(quoteId, request);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }

  // 삭제
  @DeleteMapping("/admin/quotes/{quoteId}")
  public ApiResponse<Void> deleteQuote(@PathVariable Long quoteId) {
    validateAdmin();

    adminQuoteService.deleteQuote(quoteId);

    return ApiResponse.ok(null);
  }

  // 숨김 목록
  @GetMapping("/admin/quotes/hidden")
  public ApiResponse<List<QuoteResponse>> getHiddenQuotes() {
    validateAdmin();

    List<Quote> hiddenQuotes = adminQuoteService.findHiddenQuotes();

    return ApiResponse.ok(hiddenQuotes.stream().map(QuoteResponse::from).toList());
  }

  // 숨김 해제
  @PostMapping("/admin/quotes/{quoteId}/restore")
  public ApiResponse<QuoteResponse> restoreHiddenQuotes(@PathVariable Long quoteId) {
    validateAdmin();

    Quote quote = adminQuoteService.cancelHidden(quoteId);

    return ApiResponse.ok(QuoteResponse.from(quote));
  }
}
