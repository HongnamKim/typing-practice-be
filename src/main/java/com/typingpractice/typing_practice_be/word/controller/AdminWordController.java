package com.typingpractice.typing_practice_be.word.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.dto.WordCreateRequest;
import com.typingpractice.typing_practice_be.word.dto.WordResponse;
import com.typingpractice.typing_practice_be.word.dto.WordUpdateRequest;
import com.typingpractice.typing_practice_be.word.service.WordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/word")
public class AdminWordController {
  private final WordService wordService;

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
