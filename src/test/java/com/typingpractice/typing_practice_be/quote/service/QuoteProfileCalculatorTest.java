package com.typingpractice.typing_practice_be.quote.service;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import com.typingpractice.typing_practice_be.quote.service.difficulty.QuoteProfileCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class QuoteProfileCalculatorTest {
	private final QuoteProfileCalculator calculator = new QuoteProfileCalculator();

	@Nested
	@DisplayName("공통 변수 계산")
	class CommonFeatures {
		@Test
		@DisplayName("문장 길이 계산 - 공백 포함")
		void length() {
			QuoteProfile profile = calculator.calculate("안녕 하세요", QuoteLanguage.KOREAN);
			assertThat(profile.getLength()).isEqualTo(6);
		}

		@Test
		@DisplayName("문장부호 비율 계산")
		void puncRate() {
			// "안녕, 세상!" → 문장부호 , ! = 2개, 길이 6
			QuoteProfile profile = calculator.calculate("안녕, 세상!", QuoteLanguage.KOREAN);
			assertThat(profile.getPuncRate()).isCloseTo(2f / 7f, within(0.001f));
		}

		@Test
		@DisplayName("띄어쓰기 비율 계산")
		void spaceRate() {
			// "가 나 다" → 공백 2개, 길이 5
			QuoteProfile profile = calculator.calculate("가 나 다", QuoteLanguage.KOREAN);
			assertThat(profile.getSpaceRate()).isCloseTo(2f / 5f, within(0.001f));
		}

		@Test
		@DisplayName("숫자 비율 계산")
		void digitRate() {
			// "가격은 3000원" → 숫자 4개, 길이 8
			QuoteProfile profile = calculator.calculate("가격은 3000원", QuoteLanguage.KOREAN);
			assertThat(profile.getDigitRate()).isCloseTo(4f / 9f, within(0.001f));
		}

		@Test
		@DisplayName("빈 문자열 처리")
		void emptyString() {
			QuoteProfile profile = calculator.calculate("", QuoteLanguage.KOREAN);
			assertThat(profile.getLength()).isEqualTo(0);
			assertThat(profile.getPuncRate()).isEqualTo(0f);
			assertThat(profile.getSpaceRate()).isEqualTo(0f);
			assertThat(profile.getDigitRate()).isEqualTo(0f);
		}
	}

	@Nested
	@DisplayName("한국어 전용 변수 계산")
	class KoreanFeatures {

		@Test
		@DisplayName("자모 복잡도 - 무받침")
		void jamoComplex_noFinal() {
			// "가나다" → 모두 무받침, jamoComplex = 0
			QuoteProfile profile = calculator.calculate("가나다", QuoteLanguage.KOREAN);
			assertThat(profile.getJamoComplex()).isEqualTo(0f);
		}

		@Test
		@DisplayName("자모 복잡도 - 단일받침")
		void jamoComplex_singleFinal() {
			// "간단한" → 간(ㄴ), 단(ㄴ), 한(ㄴ) 모두 단일받침
			// jamoComplexSum = 3 * 1.0 = 3.0, 평균 = 3.0/3 = 1.0, /1.5 = 0.667
			QuoteProfile profile = calculator.calculate("간단한", QuoteLanguage.KOREAN);
			assertThat(profile.getJamoComplex()).isCloseTo(1.0f / 1.5f, within(0.001f));
		}

		@Test
		@DisplayName("자모 복잡도 - 겹받침")
		void jamoComplex_doubleFinal() {
			// "읽닭" → 읽(ㄺ, 겹받침), 닭(ㄺ, 겹받침 아님 - ㄱ 단일)
			// 읽: ㄺ → 겹받침 1.5, 닭: ㄹ+ㄱ = ㄺ → 겹받침 1.5
			// 평균 = 3.0/2 = 1.5, /1.5 = 1.0
			QuoteProfile profile = calculator.calculate("읽닭", QuoteLanguage.KOREAN);
			assertThat(profile.getJamoComplex()).isCloseTo(1.0f, within(0.001f));
		}

		@Test
		@DisplayName("겹모음 비율")
		void diphthongRate() {
			// "화과" → 화(ㅘ, 겹모음), 과(ㅘ, 겹모음), koChars = 2
			QuoteProfile profile = calculator.calculate("화과", QuoteLanguage.KOREAN);
			assertThat(profile.getDiphthongRate()).isCloseTo(1.0f, within(0.001f));
		}

		@Test
		@DisplayName("겹모음 없는 문장")
		void diphthongRate_none() {
			// "가나다" → 겹모음 없음
			QuoteProfile profile = calculator.calculate("가나다", QuoteLanguage.KOREAN);
			assertThat(profile.getDiphthongRate()).isEqualTo(0f);
		}

		@Test
		@DisplayName("Shift 자모 비율 - 쌍자음")
		void shiftJamoRate_doubleConsonant() {
			// "빠른" → 빠(ㅃ, shift 초성), 른(일반), koChars = 2
			QuoteProfile profile = calculator.calculate("빠른", QuoteLanguage.KOREAN);
			assertThat(profile.getShiftJamoRate()).isCloseTo(1f / 2f, within(0.001f));
		}

		@Test
		@DisplayName("Shift 자모 비율 - 특수모음 ㅒ ㅖ")
		void shiftJamoRate_specialVowel() {
			// "얘기" → 얘(ㅒ, shift 모음), 기(일반), koChars = 2
			QuoteProfile profile = calculator.calculate("얘기", QuoteLanguage.KOREAN);
			assertThat(profile.getShiftJamoRate()).isCloseTo(1f / 2f, within(0.001f));
		}

		@Test
		@DisplayName("영어 전용 필드는 null")
		void englishFieldsNull() {
			QuoteProfile profile = calculator.calculate("안녕하세요", QuoteLanguage.KOREAN);
			assertThat(profile.getCaseFlipRate()).isNull();
			assertThat(profile.getAvgWordLen()).isNull();
		}
	}

	@Nested
	@DisplayName("영어 전용 변수 계산")
	class EnglishFeatures {

		@Test
		@DisplayName("대소문자 전환율")
		void caseFlipRate() {
			// "AbCd" → A→b(전환), b→C(전환), C→d(전환) = 3회, 길이 4
			QuoteProfile profile = calculator.calculate("AbCd", QuoteLanguage.ENGLISH);
			assertThat(profile.getCaseFlipRate()).isCloseTo(3f / 4f, within(0.001f));
		}

		@Test
		@DisplayName("대소문자 전환 없음")
		void caseFlipRate_noFlip() {
			// "hello" → 전환 없음
			QuoteProfile profile = calculator.calculate("hello", QuoteLanguage.ENGLISH);
			assertThat(profile.getCaseFlipRate()).isEqualTo(0f);
		}

		@Test
		@DisplayName("대소문자 전환 - 공백 사이는 전환으로 안 셈")
		void caseFlipRate_withSpace() {
			// "Hello World" → H→e(전환), l→l(X), o→ (비문자), W→o(전환) ...
			// 문자 간 전환만 체크, 공백은 isLetter가 false이므로 건너뜀
			QuoteProfile profile = calculator.calculate("Hello World", QuoteLanguage.ENGLISH);
			// H→e(전환), e→l(X), l→l(X), l→o(X), o→' '(건너뜀), ' '→W(건너뜀), W→o(전환), o→r(X), r→l(X), l→d(X)
			// 전환 2회, 길이 11
			assertThat(profile.getCaseFlipRate()).isCloseTo(2f / 11f, within(0.001f));
		}

		@Test
		@DisplayName("단어 평균 길이")
		void avgWordLen() {
			// "I am good" → 단어 3개, 총 문자(공백 제외) 1+2+4 = 7, 평균 7/3
			QuoteProfile profile = calculator.calculate("I am good", QuoteLanguage.ENGLISH);
			assertThat(profile.getAvgWordLen()).isCloseTo(7f / 3f, within(0.001f));
		}

		@Test
		@DisplayName("한국어 전용 필드는 null")
		void koreanFieldsNull() {
			QuoteProfile profile = calculator.calculate("hello world", QuoteLanguage.ENGLISH);
			assertThat(profile.getJamoComplex()).isNull();
			assertThat(profile.getDiphthongRate()).isNull();
			assertThat(profile.getShiftJamoRate()).isNull();
		}
	}
}
