package com.typingpractice.typing_practice_be.statistics.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.service.difficulty.DifficultyBatchService;
import com.typingpractice.typing_practice_be.quote.statistics.service.GlobalQuoteStatisticsBatchService;
import com.typingpractice.typing_practice_be.statistics.dto.MemberStatsDayRequest;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.MemberDailyStatsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.MemberTypingStatsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.MemberTypoStatsBatchService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.QuoteTypingStatsBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class AdminStatisticsController {
	private final GlobalQuoteStatisticsBatchService globalQuoteStatisticsBatchService;
	private final QuoteTypingStatsBatchService quoteTypingStatsBatchService;
	private final MemberTypingStatsBatchService memberTypingStatsBatchService;
	private final MemberDailyStatsBatchService memberDailyStatsBatchService;
	private final MemberTypoStatsBatchService memberTypoStatsBatchService;
	private final DifficultyBatchService difficultyBatchService;

	@PostMapping("/global-quote/recalculate")
	public ApiResponse<Void> recalculate() {
		globalQuoteStatisticsBatchService.runManualRecalculation();
		return ApiResponse.ok(null);
	}

	@PostMapping("/quote-typing/recalculate")
	public ApiResponse<Void> recalculateQuoteTypingStats() {
		quoteTypingStatsBatchService.runManualRecalculation();
		return ApiResponse.ok(null);
	}

	@PostMapping("/difficulty/recalculate")
	public ApiResponse<Void> recalculateDynamicDifficulty() {
		quoteTypingStatsBatchService.runManualRecalculation();
		globalQuoteStatisticsBatchService.runManualRecalculation();
		difficultyBatchService.runDynamicDifficultyBatch();
		return ApiResponse.ok(null);
	}

	@PostMapping("/member-typing/recalculate")
	public ApiResponse<Void> recalculateMemberTypingStats() {

		memberTypingStatsBatchService.runManualRecalculation();

		return ApiResponse.ok(null);
	}

	@PostMapping("/member-daily/recalculate")
	public ApiResponse<Void> recalculateMemberDailyStats(
					@ModelAttribute @Valid MemberStatsDayRequest request) {

		memberDailyStatsBatchService.runRecalculationForDate(request.getDate());

		return ApiResponse.ok(null);
	}

	@PostMapping("/member-typo/recalculate")
	public ApiResponse<Void> recalculateMemberTypoStats() {
		memberTypoStatsBatchService.runManualRecalculation();
		return ApiResponse.ok(null);
	}
}
