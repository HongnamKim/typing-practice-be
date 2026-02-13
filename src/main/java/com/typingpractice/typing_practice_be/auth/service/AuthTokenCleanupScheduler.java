package com.typingpractice.typing_practice_be.auth.service;

import com.typingpractice.typing_practice_be.auth.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuthTokenCleanupScheduler {
  private final RefreshTokenRepository refreshTokenRepository;

  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void cleanupExpiredTokens() {
    refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now(ZoneOffset.UTC));
  }
}
