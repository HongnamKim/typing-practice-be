package com.typingpractice.typing_practice_be.word.service.difficulty;

import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.word.domain.WordProfile;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class WordProfileCalculator {
  private static final Set<Character> DIPHTHONGS = Set.of('ㅘ', 'ㅙ', 'ㅚ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅢ');
  private static final Set<Character> SHIFT_INITIALS = Set.of('ㄲ', 'ㄸ', 'ㅃ', 'ㅆ', 'ㅉ');
  private static final Set<Character> SHIFT_VOWELS = Set.of('ㅒ', 'ㅖ');
  private static final Set<Character> SHIFT_FINALS = Set.of('ㄲ', 'ㄸ', 'ㅃ', 'ㅆ', 'ㅉ');
  private static final Set<Character> DOUBLE_FINALS =
      Set.of('ㄳ', 'ㄵ', 'ㄶ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅄ');

  private static final char[] INITIALS = {
    'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
  };
  private static final char[] MEDIALS = {
    'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ',
    'ㅢ', 'ㅣ'
  };
  private static final char[] FINALS = {
    0, 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ',
    'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
  };

  public WordProfile calculate(String word, WordLanguage language) {
    WordProfile profile = WordProfile.create();
    profile.setLength(word.length());

    if (language == WordLanguage.KOREAN) {
      calculateKoreanFeatures(word, profile);
    } else {
      calculateEnglishFeatures(word, profile);
    }

    return profile;
  }

  private void calculateKoreanFeatures(String word, WordProfile profile) {
    int koChars = 0;
    float jamoComplexSum = 0;
    int diphthongCount = 0;
    int shiftJamoCount = 0;

    for (char c : word.toCharArray()) {
      if (c >= '가' && c <= '힣') {
        koChars++;
        int code = c - '가';
        int initialIndex = code / (21 * 28);
        int medialIndex = (code % (21 * 28)) / 28;
        int finalIndex = code % 28;

        char initial = INITIALS[initialIndex];
        char medial = MEDIALS[medialIndex];
        char finalChar = finalIndex > 0 ? FINALS[finalIndex] : 0;

        if (finalIndex == 0) {
          // 무받침
        } else if (DOUBLE_FINALS.contains(finalChar)) {
          jamoComplexSum += 1.5f;
        } else {
          jamoComplexSum += 1.0f;
        }

        if (DIPHTHONGS.contains(medial)) diphthongCount++;
        if (SHIFT_INITIALS.contains(initial)
            || SHIFT_VOWELS.contains(medial)
            || SHIFT_FINALS.contains(finalChar)) {
          shiftJamoCount++;
        }
      }
    }

    if (koChars > 0) {
      profile.setJamoComplex((jamoComplexSum / koChars) / 1.5f);
      profile.setDiphthongRate((float) diphthongCount / koChars);
      profile.setShiftJamoRate((float) shiftJamoCount / koChars);
    } else {
      profile.setJamoComplex(0f);
      profile.setDiphthongRate(0f);
      profile.setShiftJamoRate(0f);
    }
  }

  private void calculateEnglishFeatures(String word, WordProfile profile) {
    int flipCount = 0;
    char[] chars = word.toCharArray();

    for (int i = 1; i < chars.length; i++) {
      if (Character.isLetter(chars[i]) && Character.isLetter(chars[i - 1])) {
        if (Character.isUpperCase(chars[i]) != Character.isUpperCase(chars[i - 1])) {
          flipCount++;
        }
      }
    }

    profile.setCaseFlipRate(chars.length > 0 ? (float) flipCount / chars.length : 0f);
  }
}
