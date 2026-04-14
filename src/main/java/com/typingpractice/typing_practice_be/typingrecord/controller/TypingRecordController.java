package com.typingpractice.typing_practice_be.typingrecord.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.typingrecord.dto.request.TypingRecordRequest;
import com.typingpractice.typing_practice_be.typingrecord.query.TypingRecordQuery;
import com.typingpractice.typing_practice_be.typingrecord.service.TypingRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TypingRecordController {
  private final TypingRecordService typingRecordService;

  private Long getMemberIdOrNull() {
    try {
      return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    } catch (Exception e) {
      return null;
    }
  }

  @PostMapping("/typing-records")
  public ApiResponse<Void> save(@RequestBody @Valid TypingRecordRequest request) {
    Long memberId = getMemberIdOrNull();

    TypingRecordQuery query = TypingRecordQuery.from(request);

    typingRecordService.save(memberId, query);

    return ApiResponse.ok(null);
  }
}
