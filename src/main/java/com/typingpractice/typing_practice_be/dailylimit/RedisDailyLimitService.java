package com.typingpractice.typing_practice_be.dailylimit;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Primary
@RequiredArgsConstructor
public class RedisDailyLimitService implements DailyLimitService {
  private final StringRedisTemplate redisTemplate;
  private final MemberRepository memberRepository;

  private String quoteUploadKey(Long memberId) {
    return "daily-limit:quote-upload:" + memberId + ":" + LocalDate.now();
  }

  private String reportKey(Long memberId) {
    return "daily-limit:report:" + memberId + ":" + LocalDate.now();
  }

  private Duration ttlUntilMidnight() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT);
    return Duration.between(now, midnight);
  }

  public boolean tryIncrementReportCount(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    if (member.getRole() == MemberRole.ADMIN) {
      return true;
    }

    String key = reportKey(memberId);
    Long count = redisTemplate.opsForValue().increment(key);

    if (count == null) {
      return false;
    }

    if (count == 1) {
      redisTemplate.expire(key, ttlUntilMidnight());
    }

    return count <= DailyLimitPolicy.MAX_QUOTE_UPLOAD;
  }

  @Override
  public boolean tryIncrementQuoteUploadCount(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    if (member.getRole() == MemberRole.ADMIN) {
      return true;
    }

    String key = quoteUploadKey(memberId);
    Long count = redisTemplate.opsForValue().increment(key);

    if (count == null) {
      return false;
    }

    if (count == 1) {
      redisTemplate.expire(key, ttlUntilMidnight());
    }

    return count <= DailyLimitPolicy.MAX_QUOTE_UPLOAD;
  }

  @Override
  public boolean canReport(Long memberId) {
    String key = reportKey(memberId);
    String value = redisTemplate.opsForValue().get(key);

    if (value == null) {
      return true;
    }

    return Integer.parseInt(value) < DailyLimitPolicy.MAX_REPORT;
  }

  @Override
  public boolean canUploadQuote(Long memberId) {
    return false;
  }

  @Override
  public void incrementQuoteUploadCount(Long memberId) {}
}
