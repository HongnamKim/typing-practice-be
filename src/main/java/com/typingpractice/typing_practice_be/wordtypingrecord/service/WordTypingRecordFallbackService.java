package com.typingpractice.typing_practice_be.wordtypingrecord.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typingpractice.typing_practice_be.wordtypingrecord.domain.WordTypingRecord;
import com.typingpractice.typing_practice_be.wordtypingrecord.repository.WordTypingRecordRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WordTypingRecordFallbackService {
  private final WordTypingRecordRepository repository;
  private final ObjectMapper objectMapper;
  private final Path fallbackFilePath;
  private final AtomicBoolean flushing = new AtomicBoolean(false);

  public WordTypingRecordFallbackService(
      WordTypingRecordRepository repository,
      @Value(
              "${fallback.word-typing-record.path:/data/fallback/fallback-word-typing-records.jsonl}")
          String path) {
    this.repository = repository;
    this.fallbackFilePath = Paths.get(path);
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  public void writeToFile(WordTypingRecord record) {
    try {
      Files.createDirectories(fallbackFilePath.getParent());
      String json = objectMapper.writeValueAsString(record);
      Files.writeString(
          fallbackFilePath, json + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      if (record.getMemberId() != null) {
        log.warn(
            "[WordFallback] 로컬 파일에 저장: memberId={}, language={}",
            record.getMemberId(),
            record.getLanguage());
      } else {
        log.warn(
            "[WordFallback] 로컬 파일에 저장: anonymousId={}, language={}",
            record.getAnonymousId(),
            record.getLanguage());
      }
    } catch (IOException e) {
      log.error("[WordFallback] 파일 쓰기 실패 - 기록 유실: {}", record.toString());
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
      List<String> failedLines = new ArrayList<>();
      int successCount = 0;

      for (String line : lines) {
        if (line.isBlank()) continue;
        try {
          WordTypingRecord record = objectMapper.readValue(line, WordTypingRecord.class);
          repository.save(record);
          successCount++;
        } catch (Exception e) {
          log.error("[WordFallback] flush 중 레코드 저장 실패: {}", e.getMessage());
          failedLines.add(line);
        }
      }

      if (failedLines.isEmpty()) {
        Files.deleteIfExists(fallbackFilePath);
      } else {
        Files.writeString(fallbackFilePath, String.join("\n", failedLines) + "\n");
        log.warn("[WordFallback] {}건 flush 실패 - 파일에 유지", failedLines.size());
      }

      log.info("[WordFallback] flush 완료 - {}건 복구", successCount);

    } catch (IOException e) {
      log.error("[WordFallback] flush 실패: {}", e.getMessage());
    } finally {
      flushing.set(false);
    }
  }
}
