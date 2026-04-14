package com.typingpractice.typing_practice_be.common.utils;

import java.time.*;

public class TimeUtils {
  public static final String KST_ZONE = "Asia/Seoul";
  public static final ZoneId KST = ZoneId.of(KST_ZONE);

  public static final String KST_OFFSET = "+09:00";

  public static ZoneId parseZoneId(String timezone) {
    try {
      return ZoneId.of(timezone);
    } catch (DateTimeException e) {
      throw new IllegalArgumentException("유효하지 않은 timezone: " + timezone);
    }
  }

  // KST 날짜의 시작 시각을 UTC로
  public static LocalDateTime startOfDayKstToUtc(LocalDate kstDate) {
    return kstDate.atStartOfDay(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
  }

  // KST 날짜의 끝 시각을 UTC로
  public static LocalDateTime endOfDayKstToUtc(LocalDate kstDate) {
    return kstDate
        .atTime(LocalTime.MAX)
        .atZone(KST)
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalDateTime();
  }

  // 임의 타임존 날짜의 시작 시각을 UTC로
  public static LocalDateTime startOfDayToUtc(LocalDate date, ZoneId zone) {
    return date.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
  }

  // 임의 타임존 날짜의 끝 시각을 UTC로
  public static LocalDateTime endOfDayToUtc(LocalDate date, ZoneId zone) {
    return date.atTime(LocalTime.MAX)
        .atZone(zone)
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalDateTime();
  }
}
