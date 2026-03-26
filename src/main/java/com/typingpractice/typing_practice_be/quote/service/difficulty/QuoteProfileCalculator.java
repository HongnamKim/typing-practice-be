package com.typingpractice.typing_practice_be.quote.service.difficulty;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class QuoteProfileCalculator {

	// 겹모음 목록: ㅘ ㅙ ㅚ ㅝ ㅞ ㅟ ㅢ
	private static final Set<Character> DIPHTHONGS = Set.of('ㅘ', 'ㅙ', 'ㅚ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅢ');

	// 쌍자음: ㄲ ㄸ ㅃ ㅆ ㅉ, 특수모음: ㅒ ㅖ
	private static final Set<Character> SHIFT_INITIALS = Set.of('ㄲ', 'ㄸ', 'ㅃ', 'ㅆ', 'ㅉ');
	private static final Set<Character> SHIFT_VOWELS = Set.of('ㅒ', 'ㅖ');
	private static final Set<Character> SHIFT_FINALS = Set.of('ㄲ', 'ㄸ', 'ㅃ', 'ㅆ', 'ㅉ');

	// 겹받침 목록
	private static final Set<Character> DOUBLE_FINALS =
					Set.of('ㄳ', 'ㄵ', 'ㄶ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅄ');

	// 초성 인덱스 → 자모 매핑
	private static final char[] INITIALS = {
					'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
	};

	// 중성 인덱스 → 자모 매핑
	private static final char[] MEDIALS = {
					'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ',
					'ㅢ', 'ㅣ'
	};

	// 종성 인덱스 → 자모 매핑 (0은 받침 없음)
	private static final char[] FINALS = {
					0, 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ',
					'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
	};

	private char getInitial(int index) {
		return INITIALS[index];
	}

	private char getMedial(int index) {
		return MEDIALS[index];
	}

	private char getFinal(int index) {
		return FINALS[index];
	}

	public QuoteProfile calculate(String sentence, QuoteLanguage language) {
		// 공통: length, puncRate, spaceRate, digitRate
		// 한국어: jamoComplex, diphthongRate, shiftJamoRate
		// 영어: caseFlipRate, avgWordLen
		// difficultySeed 는 DifficultySeedCalculator 에서 설정
		QuoteProfile profile = QuoteProfile.create();

		int len = sentence.length();
		profile.setLength(len);

		// 공통 변수
		int puncCount = 0;
		int spaceCount = 0;
		int digitCount = 0;

		for (char c : sentence.toCharArray()) {
			// 특수 기호 체크
			if (Character.getType(c) == Character.OTHER_PUNCTUATION
							|| Character.getType(c) == Character.MATH_SYMBOL
							|| Character.getType(c) == Character.CURRENCY_SYMBOL
							|| Character.getType(c) == Character.OTHER_SYMBOL
							|| Character.getType(c) == Character.START_PUNCTUATION
							|| Character.getType(c) == Character.END_PUNCTUATION
							|| Character.getType(c) == Character.CONNECTOR_PUNCTUATION
							|| Character.getType(c) == Character.DASH_PUNCTUATION
							|| Character.getType(c) == Character.INITIAL_QUOTE_PUNCTUATION
							|| Character.getType(c) == Character.FINAL_QUOTE_PUNCTUATION) {
				puncCount++;
			}
			// 띄어쓰기 체크
			if (c == ' ') spaceCount++;
			// 숫자 체크
			if (Character.isDigit(c)) digitCount++;
		}
		profile.setPuncRate(len > 0 ? (float) puncCount / len : 0f);
		profile.setSpaceRate(len > 0 ? (float) spaceCount / len : 0f);
		profile.setDigitRate(len > 0 ? (float) digitCount / len : 0f);

		if (language == QuoteLanguage.KOREAN) {
			// 한국어 전용 특성
			calculateKoreanFeatures(sentence, profile);
		} else {
			// 영어 전용 특성
			calculateEnglishFeatures(sentence, profile);
		}

		return profile;
	}

	private void calculateKoreanFeatures(String sentence, QuoteProfile profile) {
		int koChars = 0;
		float jamoComplexSum = 0f;
		int diphthongCount = 0;
		int shiftJamoCount = 0;

		for (char c : sentence.toCharArray()) {
			if (c >= '가' && c <= '힣') {
				koChars++;

				int code = c - '가';
				int initialIndex = code / (21 * 28);
				int medialIndex = (code % (21 * 28)) / 28;
				int finalIndex = code % 28;

				// 초성
				char initial = getInitial(initialIndex);
				// 중성
				char medial = getMedial(medialIndex);
				// 종성
				char finalChar = finalIndex > 0 ? getFinal(finalIndex) : 0;

				// 받침 복잡도
				if (finalIndex == 0) {
					// 무받침: 0
				} else if (DOUBLE_FINALS.contains(finalChar)) {
					jamoComplexSum += 1.5f;
				} else {
					jamoComplexSum += 1.0f;
				}

				// 겹모음
				if (DIPHTHONGS.contains(medial)) {
					diphthongCount++;
				}

				// Shift 자모 (쌍자음 + 특수모음)
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

	private void calculateEnglishFeatures(String sentence, QuoteProfile profile) {
		// 대소문자 전환율
		int flipCount = 0;
		char[] chars = sentence.toCharArray();

		for (int i = 1; i < chars.length; i++) {
			if (Character.isLetter(chars[i]) && Character.isLetter(chars[i - 1])) {
				if (Character.isUpperCase(chars[i]) != Character.isUpperCase(chars[i - 1])) {
					flipCount++;
				}
			}
		}
		profile.setCaseFlipRate(chars.length > 0 ? (float) flipCount / chars.length : 0f);

		// 단어 평균 길이
		String[] words = sentence.trim().split("\\s+");
		if (words.length > 0) {
			int totalChars = 0;

			for (String word : words) {
				totalChars += word.length();
			}

			profile.setAvgWordLen((float) totalChars / words.length);
		} else {
			profile.setAvgWordLen(0f);
		}
	}
}
