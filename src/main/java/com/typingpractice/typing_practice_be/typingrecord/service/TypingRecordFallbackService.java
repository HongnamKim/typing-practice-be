package com.typingpractice.typing_practice_be.typingrecord.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typingpractice.typing_practice_be.typingrecord.domain.TypingRecord;
import com.typingpractice.typing_practice_be.typingrecord.repository.TypingRecordRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.service.TodayTypingStatsRedisService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TypingRecordFallbackService {
  private final TypingRecordRepository typingRecordRepository;
  private final TodayTypingStatsRedisService todayTypingStatsRedisService;
  private final ObjectMapper objectMapper;
  private final Path fallbackFilePath;
  private final AtomicBoolean flushing = new AtomicBoolean(false);

  public TypingRecordFallbackService(
      TypingRecordRepository typingRecordRepository,
      TodayTypingStatsRedisService todayTypingStatsRedisService,
      @Value("${fallback.typing-record.path:/data/fallback/fallback-typing-records.jsonl}")
          String path) {
    this.typingRecordRepository = typingRecordRepository;
    this.todayTypingStatsRedisService = todayTypingStatsRedisService;
    this.fallbackFilePath = Paths.get(path);
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  public void writeToFile(TypingRecord record) {
    try {
      Files.createDirectories(fallbackFilePath.getParent());
      String json = objectMapper.writeValueAsString(record);
      Files.writeString(
          fallbackFilePath, json + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

      if (record.getMemberId() != null) {
        log.warn(
            "[Fallback] 로컬 파일에 저장: quoteId={}, memberId={}",
            record.getQuoteId(),
            record.getMemberId());
      } else {
        log.warn(
            "[Fallback] 로컬 파일에 저장: quoteId={}, anonymousId={}",
            record.getQuoteId(),
            record.getAnonymousId());
      }

    } catch (IOException e) {
      log.error("[Fallback] 파일 쓰기 실패 - 기록 유실: {}", record.toString());
    }
  }

  public boolean hasBacklog() {
    return Files.exists(fallbackFilePath);
  }

  public void flushIfNeeded() {
    if (!hasBacklog()) return;
    if (!flushing.compareAndSet(false, true)) return;

    try {
      List<String> lines = Files.readAllLines(fallbackFilePath);
      Set<Long> affectedMemberIds = new HashSet<>();
      int successCount = 0;

      List<String> failedLines = new ArrayList<>();

      for (String line : lines) {
        if (line.isBlank()) continue;
        try {
          TypingRecord record = objectMapper.readValue(line, TypingRecord.class);
          typingRecordRepository.save(record);

          if (record.getMemberId() != null) {
            affectedMemberIds.add(record.getMemberId());
          }

          successCount++;

        } catch (Exception e) {
          log.error("[Fallback] flush 중 레코드 저장 실패: {}", e.getMessage());
          failedLines.add(line);
        }
      }

      for (Long memberId : affectedMemberIds) {
        todayTypingStatsRedisService.invalidateAll(memberId);
      }

      if (failedLines.isEmpty()) {
        Files.deleteIfExists(fallbackFilePath);
      } else {
        Files.writeString(fallbackFilePath, String.join("\n", failedLines) + "\n");
        log.warn("[Fallback] {}건 flush 실패 - 파일에 유지", failedLines.size());
      }

      log.info(
          "[Fallback] flush 완료 - {}건 복구, Redis 무효화 {}명", successCount, affectedMemberIds.size());

    } catch (IOException e) {
      log.error("[Fallback] flush 실패: {}", e.getMessage());
    } finally {
      flushing.set(false);
    }
  }
}
