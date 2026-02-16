package com.typingpractice.typing_practice_be.quote.service;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import org.springframework.stereotype.Component;

@Component
public class QuoteProfileCalculator {

  public QuoteProfile calculate(String sentence, QuoteLanguage language) {
    // 공통: length, puncRate, spaceRate, digitRate
    // 한국어: jamoComplex, diphthongRate, shiftJamoRate
    // 영어: caseFlipRate, avgWordLen
    // difficultySeed 는 DifficultySeedCalculator 에서 설정
    return null;
  }
}
