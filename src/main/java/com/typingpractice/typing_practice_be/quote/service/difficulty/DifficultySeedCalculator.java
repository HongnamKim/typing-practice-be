package com.typingpractice.typing_practice_be.quote.service.difficulty;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import org.springframework.stereotype.Component;

@Component
public class DifficultySeedCalculator {
	// 공통 가중치
	private static final float W_LEN = 0.20f;
	private static final float W_PUNC = 0.15f;
	private static final float W_SPACE = 0.10f;
	private static final float W_DIGIT = 0.15f;

	// 한국어 전용 가중치
	private static final float W_JAMO = 0.15f;
	private static final float W_DIPHTHONG = 0.10f;
	private static final float W_SHIFT_JAMO = 0.15f;

	// 영어 전용 가중치
	private static final float W_CASE = 0.25f;
	private static final float W_WORD_LEN = 0.15f;

	public float calculate(
					QuoteProfile profile, GlobalQuoteStatistics stats, QuoteLanguage language) {
		// 공통 score 4개 + 언어 전용 score 3개
		// σ == 0이면 해당 score = 0
		// 가중합 → round(100 * sum) 반환
		float sum = 0f;

		// 공통
		sum += W_LEN * zScore(profile.getLength(), stats.getLenMean(), stats.getLenStd());
		sum += W_PUNC * zScore(profile.getPuncRate(), stats.getPuncMean(), stats.getPuncStd());
		sum +=
						W_SPACE * zScoreInverse(profile.getSpaceRate(), stats.getSpaceMean(), stats.getSpaceStd());
		sum += W_DIGIT * zScore(profile.getDigitRate(), stats.getDigitMean(), stats.getDigitStd());

		if (language == QuoteLanguage.KOREAN) {
			sum += W_JAMO * zScore(profile.getJamoComplex(), stats.getJamoMean(), stats.getJamoStd());
			sum +=
							W_DIPHTHONG
											* zScore(
											profile.getDiphthongRate(), stats.getDiphthongMean(), stats.getDiphthongStd());
			sum +=
							W_SHIFT_JAMO
											* zScore(
											profile.getShiftJamoRate(), stats.getShiftJamoMean(), stats.getShiftJamoStd());
		} else {
			sum += W_CASE * zScore(profile.getCaseFlipRate(), stats.getCaseMean(), stats.getCaseStd());
			sum +=
							W_WORD_LEN
											* zScore(profile.getAvgWordLen(), stats.getWordLenMean(), stats.getWordLenStd());
		}

		return Math.round(100 * sum);
	}

	private float zScore(float value, float mean, float std) {
		if (std == 0f) return 0f;
		return clip((value - mean) / std);
	}

	private float zScoreInverse(float value, float mean, float std) {
		if (std == 0f) return 0f;
		return clip((mean - value) / std);
	}

	private float clip(float value) {
		return Math.max(0f, Math.min(1f, (value + 2f) / 4f));
	}
}
