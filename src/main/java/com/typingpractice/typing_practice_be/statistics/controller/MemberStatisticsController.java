package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.statistics.service.MemberStatisticsService;
import com.typingpractice.typing_practice_be.typingrecord.dto.MemberTypingStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

  @PostMapping("/refresh")
  public ApiResponse<MemberTypingStatsResponse> refreshStats() {
    Long memberId = getAuthenticatedMemberId();
    return ApiResponse.ok(memberStatisticsService.refreshStats(memberId));
  }

  private Long getAuthenticatedMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
