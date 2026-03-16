package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.statistics.dto.MemberDailyStatsRequest;
import com.typingpractice.typing_practice_be.statistics.service.MemberStatisticsService;
import com.typingpractice.typing_practice_be.typingrecord.dto.MemberDailyStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.MemberTypingStatsResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.MemberTypoStatsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members/me/stats")
@RequiredArgsConstructor
public class MemberStatisticsController {
  private final MemberStatisticsService memberStatisticsService;

  @GetMapping("/typing")
  public ApiResponse<MemberTypingStatsResponse> getTypingStats() {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(memberStatisticsService.getTypingStats(memberId));
  }

  @GetMapping("/daily")
  public ApiResponse<MemberDailyStatsResponse> getDailyStats(
      @ModelAttribute @Valid MemberDailyStatsRequest request) {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(memberStatisticsService.getDailyStats(memberId, request.getDays()));
  }

  @GetMapping("/typos")
  public ApiResponse<MemberTypoStatsResponse> getTypoStats(@RequestParam QuoteLanguage language) {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(memberStatisticsService.getTypoStats(memberId, language));
  }

  @PostMapping("/refresh")
  public ApiResponse<MemberTypingStatsResponse> refreshStats() {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(memberStatisticsService.refreshStats(memberId));
  }

  private Long getAuthenticatedMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
