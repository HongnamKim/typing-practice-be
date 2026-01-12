package com.typingpractice.typing_practice_be.auth.service;

import com.typingpractice.typing_practice_be.auth.repository.JwtBlackListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class BlackListCleanupScheduler {
  private final JwtBlackListRepository jwtBlackListRepository;

  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void cleanupExpiredTokens() {
    jwtBlackListRepository.deleteExpiredTokens(LocalDateTime.now(ZoneOffset.UTC));
  }
}
