package com.typingpractice.typing_practice_be.wordtypingrecord.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.WordTypingRecordRequest;
import com.typingpractice.typing_practice_be.wordtypingrecord.query.WordTypingRecordQuery;
import com.typingpractice.typing_practice_be.wordtypingrecord.service.WordTypingRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WordTypingRecordController {
  private final WordTypingRecordService wordTypingRecordService;

  private Long getMemberIdOrNull() {
    try {
      return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    } catch (Exception e) {
      return null;
    }
  }

  @PostMapping("/word-typing-records")
  public ApiResponse<Void> save(@RequestBody @Valid WordTypingRecordRequest request) {
    Long memberId = getMemberIdOrNull();

    WordTypingRecordQuery query = WordTypingRecordQuery.from(request);

    wordTypingRecordService.save(memberId, query);

    return ApiResponse.ok(null);
  }
}
