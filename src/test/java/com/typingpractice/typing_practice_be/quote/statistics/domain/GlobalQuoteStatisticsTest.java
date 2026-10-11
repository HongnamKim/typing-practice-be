package com.typingpractice.typing_practice_be.quote.statistics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.typingpractice.typing_practice_be.quote.service.difficulty.DifficultyScale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GlobalQuoteStatisticsTest {

  @Test
  @DisplayName("표준화 통계가 없으면(null) 기본값을 돌려준다")
  void defaultsWhenNull() {
    GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();

    assertThat(stats.getSeedMeanOrDefault()).isEqualTo(DifficultyScale.DEFAULT_SEED_MEAN);
    assertThat(stats.getSeedStdOrDefault()).isEqualTo(DifficultyScale.DEFAULT_SEED_STD);
    assertThat(stats.getPerfMeanOrDefault()).isEqualTo(DifficultyScale.DEFAULT_PERF_MEAN);
    assertThat(stats.getPerfStdOrDefault()).isEqualTo(DifficultyScale.DEFAULT_PERF_STD);
  }

  @Test
  @DisplayName("저장된 표준화 통계를 돌려준다")
  void storedValues() {
    GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
    stats.updateSeedDistribution(0.1f, 0.3f);
    stats.updatePerfDistribution(-0.01f, 0.08f);

    assertThat(stats.getSeedMeanOrDefault()).isEqualTo(0.1f);
    assertThat(stats.getSeedStdOrDefault()).isEqualTo(0.3f);
    assertThat(stats.getPerfMeanOrDefault()).isEqualTo(-0.01f);
    assertThat(stats.getPerfStdOrDefault()).isEqualTo(0.08f);
  }

  @Test
  @DisplayName("std 가 0 이면 mean 과 std 모두 기본값으로 떨어진다 (0 으로 나누기 방지)")
  void zeroStdFallsBackToDefault() {
    GlobalQuoteStatistics stats = GlobalQuoteStatistics.createKoreanDefault();
    stats.updateSeedDistribution(0.1f, 0f);
    stats.updatePerfDistribution(0.1f, 0f);

    assertThat(stats.getSeedMeanOrDefault()).isEqualTo(DifficultyScale.DEFAULT_SEED_MEAN);
    assertThat(stats.getSeedStdOrDefault()).isEqualTo(DifficultyScale.DEFAULT_SEED_STD);
    assertThat(stats.getPerfMeanOrDefault()).isEqualTo(DifficultyScale.DEFAULT_PERF_MEAN);
    assertThat(stats.getPerfStdOrDefault()).isEqualTo(DifficultyScale.DEFAULT_PERF_STD);
  }

  @Test
  @DisplayName("copySeedDistributionFrom: 이전 행의 seed 통계를 복사하고, null 이면 무시한다")
  void copySeedDistribution() {
    GlobalQuoteStatistics prev = GlobalQuoteStatistics.createKoreanDefault();
    prev.updateSeedDistribution(0.1f, 0.3f);
    GlobalQuoteStatistics next = GlobalQuoteStatistics.createKoreanDefault();

    next.copySeedDistributionFrom(prev);
    assertThat(next.getSeedMeanOrDefault()).isEqualTo(0.1f);
    assertThat(next.getSeedStdOrDefault()).isEqualTo(0.3f);

    next.copySeedDistributionFrom(null);
    assertThat(next.getSeedStdOrDefault()).isEqualTo(0.3f);
  }
}
