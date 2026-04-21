package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.statistics.service.MemberWordStatisticsService;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.MemberWordTypingStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/me/word-stats")
@RequiredArgsConstructor
public class MemberWordStatisticsController {
  private final MemberWordStatisticsService memberWordStatisticsService;

  private Long getMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }

  @GetMapping("/typing")
  public ApiResponse<MemberWordTypingStatsResponse> getTypingStats(
      @RequestParam WordLanguage language) {
    Long memberId = getMemberId();
    return ApiResponse.ok(memberWordStatisticsService.getTypingStats(memberId, language));
  }
}
