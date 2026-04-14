package com.typingpractice.typing_practice_be.adaptiveserving.controller;

import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveQuoteListResponse;
import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveQuoteRequest;
import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveQuoteResponse;
import com.typingpractice.typing_practice_be.adaptiveserving.service.AdaptiveQuoteService;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdaptiveServingController {
  private final AdaptiveQuoteService adaptiveQuoteService;

  private Long getMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }

  @GetMapping("/quotes/adaptive")
  public ApiResponse<AdaptiveQuoteListResponse> getAdaptiveQuotes(
      @ModelAttribute @Valid AdaptiveQuoteRequest request) {
    Long memberId = getMemberId();

    List<AdaptiveQuoteResponse> quotes =
        adaptiveQuoteService.getAdaptiveQuotes(
            memberId, request.getLanguage(), request.getCount(), request.getExcludeIds());

    return ApiResponse.ok(AdaptiveQuoteListResponse.of(quotes));
  }
}
