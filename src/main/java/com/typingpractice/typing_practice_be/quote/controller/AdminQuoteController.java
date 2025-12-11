package com.typingpractice.typing_practice_be.quote.controller;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.exception.NotAdminException;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import com.typingpractice.typing_practice_be.quote.service.AdminQuoteService;
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
  public void getPendingQuotes() {
    validateAdmin();
  }

  // 승인
  @PostMapping("/admin/quotes/{quoteId}/approve")
  public void approvePendingQuote(@PathVariable Long quoteId) {
    validateAdmin();
  }

  // 거부
  @PostMapping("/admin/quotes/{quoteId}/reject")
  public void rejectPendingQuote(@PathVariable Long quoteId) {
    validateAdmin();
  }

  // 공개 문장 수정
  @PatchMapping("/admin/quotes/{quoteId}")
  public void patchQuote(@PathVariable Long quoteId) {
    validateAdmin();
  }

  // 삭제
  @DeleteMapping("/admin/quotes/{quoteId}")
  public void deleteQuote(@PathVariable Long quoteId) {
    validateAdmin();
  }

  // 숨김 목록
  @GetMapping("/admin/quotes/hidden")
  public void getHiddenQuotes() {
    validateAdmin();
  }

  // 숨김 해제
  @PostMapping("/admin/quotes/{quoteId}/restore")
  public void restoreHiddenQuotes(@PathVariable Long quoteId) {
    validateAdmin();
  }
}
