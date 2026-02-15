package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.exception.QuoteLanguageMismatchException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class QuoteLanguageValidator {
  private static final Pattern ENGLISH_LETTER = Pattern.compile("[A-Za-z]");
  private static final Pattern KOREAN_LETTER = Pattern.compile("[가-힣ㄱ-ㅎㅏ-ㅣ]");

  public void validate(String sentence, QuoteLanguage language) {
    if (language == QuoteLanguage.KOREAN && ENGLISH_LETTER.matcher(sentence).find()) {
      throw new QuoteLanguageMismatchException();
    }

    if (language == QuoteLanguage.ENGLISH && KOREAN_LETTER.matcher(sentence).find()) {
      throw new QuoteLanguageMismatchException();
    }
  }
}
