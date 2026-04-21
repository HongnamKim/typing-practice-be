package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.dto.MemberDailyStatsRequest;
import com.typingpractice.typing_practice_be.statistics.service.MemberQuoteStatisticsService;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberDailyStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypingStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoDetailStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoStatsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members/me/stats")
@RequiredArgsConstructor
public class MemberQuoteStatisticsController {
  private final MemberQuoteStatisticsService memberQuoteStatisticsService;

  @GetMapping("/typing")
  public ApiResponse<MemberTypingStatsResponse> getTypingStats(
      @RequestParam QuoteLanguage language) {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(memberQuoteStatisticsService.getTypingStats(memberId, language));
  }

  @GetMapping("/daily")
  public ApiResponse<MemberDailyStatsResponse> getDailyStats(
      @ModelAttribute @Valid MemberDailyStatsRequest request) {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(
        memberQuoteStatisticsService.getDailyStats(
            memberId, request.getLanguage(), request.getDays()));
  }

  @GetMapping("/typos")
  public ApiResponse<MemberTypoStatsResponse> getTypoStats(@RequestParam QuoteLanguage language) {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(memberQuoteStatisticsService.getTypoStats(memberId, language));
  }

  @GetMapping("/typos/detail")
  public ApiResponse<MemberTypoDetailStatsResponse> getTypoDetailStats(
      @RequestParam QuoteLanguage language, @RequestParam String expected) {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(
        memberQuoteStatisticsService.getTypoDetailStats(memberId, language, expected));
  }

  @PostMapping("/refresh")
  public ApiResponse<MemberTypingStatsResponse> refreshStats(@RequestParam QuoteLanguage language) {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(memberQuoteStatisticsService.refreshStats(memberId, language));
  }

  private Long getAuthenticatedMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
