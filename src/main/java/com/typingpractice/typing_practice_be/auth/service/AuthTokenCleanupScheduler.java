package com.typingpractice.typing_practice_be.auth.service;

import com.typingpractice.typing_practice_be.auth.repository.JwtBlackListRepository;
import com.typingpractice.typing_practice_be.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class AuthTokenCleanupScheduler {
  private final JwtBlackListRepository jwtBlackListRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void cleanupExpiredTokens() {
    refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now(ZoneOffset.UTC));
  }

  @Scheduled(cron = "0 0 4 * * *")
  @Transactional
  public void cleanupBlackListTokens() {
    jwtBlackListRepository.deleteExpiredTokens(LocalDateTime.now(ZoneOffset.UTC));
  }
}
