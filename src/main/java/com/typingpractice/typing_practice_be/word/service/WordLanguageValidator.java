package com.typingpractice.typing_practice_be.word.service;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.exception.WordLanguageMismatchException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class WordLanguageValidator {
  private static final Pattern ENGLISH_LETTER = Pattern.compile("[A-Za-z]");
  private static final Pattern KOREAN_LETTER = Pattern.compile("[가-힣ㄱ-ㅎㅏ-ㅣ]");

  public void validate(String word, WordLanguage language) {
    if (language == WordLanguage.KOREAN && ENGLISH_LETTER.matcher(word).find()) {
      throw new WordLanguageMismatchException();
    }

    if (language == WordLanguage.ENGLISH && KOREAN_LETTER.matcher(word).find()) {
      throw new WordLanguageMismatchException();
    }
  }
}
