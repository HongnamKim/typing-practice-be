package com.typingpractice.typing_practice_be.word.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.word.domain.Word;
import com.typingpractice.typing_practice_be.word.dto.WordRequest;
import com.typingpractice.typing_practice_be.word.dto.WordResponse;
import com.typingpractice.typing_practice_be.word.service.WordService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/words")
public class WordController {
  private final WordService wordService;

  @GetMapping()
  public ApiResponse<List<WordResponse>> getRandomWords(
      @ModelAttribute @Valid WordRequest request) {
    List<Word> words =
        wordService.findWords(request.getLanguage(), request.getDifficulty(), request.getCount());

    List<WordResponse> response = words.stream().map(WordResponse::from).toList();

    return ApiResponse.ok(response);
  }
}
