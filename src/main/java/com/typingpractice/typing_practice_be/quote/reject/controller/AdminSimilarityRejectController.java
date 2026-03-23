package com.typingpractice.typing_practice_be.quote.reject.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.common.utils.TimeUtils;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.reject.domain.QuoteSimilarityRejectLog;
import com.typingpractice.typing_practice_be.quote.reject.dto.RejectSummaryResponse;
import com.typingpractice.typing_practice_be.quote.reject.repository.QuoteSimilarityRejectLogRepository;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stats/similarity-rejects")
@RequiredArgsConstructor
public class AdminSimilarityRejectController {
  private final QuoteSimilarityRejectLogRepository rejectLogRepository;
  private final QuoteRepository quoteRepository;

  @GetMapping
  public ApiResponse<PageResult<QuoteSimilarityRejectLog>> getRejects(
      @RequestParam QuoteLanguage language,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = TimeUtils.KST_ZONE) String timezone,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "50") int size) {

    if (startDate.isAfter(endDate)) {
      throw new IllegalArgumentException("startDate는 endDate보다 같거나 이전이어야 합니다.");
    }

    ZoneId zone = TimeUtils.parseZoneId(timezone);
    LocalDateTime from = TimeUtils.startOfDayToUtc(startDate, zone);
    LocalDateTime to = TimeUtils.endOfDayToUtc(endDate, zone);

    List<QuoteSimilarityRejectLog> logs =
        rejectLogRepository.findByPeriod(language, from, to, page, size);

    boolean hasNext = logs.size() > size;
    List<QuoteSimilarityRejectLog> content = hasNext ? logs.subList(0, size) : logs;

    return ApiResponse.ok(new PageResult<>(content, page, size, hasNext));
  }

  @GetMapping("/summary")
  public ApiResponse<RejectSummaryResponse> getSummary(
      @RequestParam QuoteLanguage language,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = TimeUtils.KST_ZONE) String timezone) {
    if (startDate.isAfter(endDate)) {
      throw new IllegalArgumentException("startDate는 endDate보다 같거나 이전이어야 합니다.");
    }

    ZoneId zone = TimeUtils.parseZoneId(timezone);
    LocalDateTime from = TimeUtils.startOfDayToUtc(startDate, zone);
    LocalDateTime to = TimeUtils.endOfDayToUtc(endDate, zone);

    long rejectCount = rejectLogRepository.countByPeriod(language, from, to);
    long uploadCount = quoteRepository.countByPeriod(language, from, to);
    long totalAttempts = uploadCount + rejectCount;
    double rejectRate = totalAttempts > 0 ? (double) rejectCount / totalAttempts : 0.0;
    Double avgSimilarity = rejectLogRepository.avgSimilarityByPeriod(language, from, to);

    return ApiResponse.ok(
        RejectSummaryResponse.create(
            rejectCount, totalAttempts, rejectRate, avgSimilarity != null ? avgSimilarity : 0.0));
  }
}
