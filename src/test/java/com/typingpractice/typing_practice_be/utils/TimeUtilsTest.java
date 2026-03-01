package com.typingpractice.typing_practice_be.utils;

import com.typingpractice.typing_practice_be.common.utils.TimeUtils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

public class TimeUtilsTest {
  @Test
  void startOfDayKstToUtc() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    // given
    LocalDate kstDate = LocalDate.of(2026, 3, 1);
    LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    LocalDateTime now2 = LocalDateTime.now();
    System.out.println("now = " + now);
    System.out.println("now2 = " + now2);

    // when
    LocalDateTime utcDate = TimeUtils.startOfDayKstToUtc(kstDate);

    // then
    Assertions.assertThat(kstDate.isAfter(utcDate.toLocalDate())).isTrue();
    System.out.println("kstDate = " + kstDate);
    System.out.println("utcDate = " + utcDate);
  }

  @Test
  void endOfDayKstToUtc() {
    // given
    LocalDate kstDate = LocalDate.of(2026, 3, 1);

    // when
    LocalDateTime utcDate = TimeUtils.endOfDayKstToUtc(kstDate);

    // then
    // Assertions.assertThat(kstDate.isAfter(utcDate.toLocalDate())).isTrue();
    System.out.println("kstDate = " + kstDate);
    System.out.println("utcDate = " + utcDate);
  }

  @Test
  void startOfDayToUtc() {
    // given
    LocalDate localDate = LocalDate.of(2026, 3, 1);

    // when
    LocalDateTime utcDate = TimeUtils.startOfDayToUtc(localDate, ZoneId.of("Asia/Shanghai"));

    // then
    System.out.println("localDate = " + localDate);
    System.out.println("utcDate = " + utcDate);
  }
}
