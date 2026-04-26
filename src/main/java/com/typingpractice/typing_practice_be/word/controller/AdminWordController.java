package com.typingpractice.typing_practice_be.word.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.dto.response.AdminWordPaginationResponse;
import com.typingpractice.typing_practice_be.word.dto.request.WordCreateRequest;
import com.typingpractice.typing_practice_be.word.dto.request.WordPaginationRequest;
import com.typingpractice.typing_practice_be.word.dto.response.AdminWordResponse;
import com.typingpractice.typing_practice_be.word.dto.response.WordResponse;
import com.typingpractice.typing_practice_be.word.dto.request.WordUpdateRequest;
import com.typingpractice.typing_practice_be.word.query.WordPaginationQuery;
import com.typingpractice.typing_practice_be.word.service.WordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/words")
public class AdminWordController {
  private final WordService wordService;

  @GetMapping()
  public ApiResponse<AdminWordPaginationResponse> getWords(
      @ModelAttribute @Valid WordPaginationRequest request) {
    WordPaginationQuery query = WordPaginationQuery.from(request);
    PageResult<Word> result = wordService.findAllForAdmin(query);

    return ApiResponse.ok(AdminWordPaginationResponse.from(result));
  }

  @GetMapping("/{id}")
  public ApiResponse<AdminWordResponse> getWord(@PathVariable Long id) {
    Word word = wordService.findByIdWithTypingStats(id);
    return ApiResponse.ok(AdminWordResponse.from(word));
  }

  @PostMapping()
  public ApiResponse<WordResponse> postWord(@RequestBody @Valid WordCreateRequest request) {
    Word word = wordService.createWord(request.getWord(), request.getLanguage());

    return ApiResponse.ok(WordResponse.from(word));
  }

  @PatchMapping("/{id}")
  public ApiResponse<WordResponse> patchWord(
      @PathVariable Long id, @RequestBody WordUpdateRequest request) {
    Word word = wordService.updateWord(id, request.getWord());

    return ApiResponse.ok(WordResponse.from(word));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> deleteWord(@PathVariable Long id) {
    wordService.deleteWord(id);

    return ApiResponse.ok(null);
  }
}
