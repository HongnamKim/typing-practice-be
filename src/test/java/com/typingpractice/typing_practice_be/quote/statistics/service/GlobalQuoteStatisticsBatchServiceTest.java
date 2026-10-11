package com.typingpractice.typing_practice_be.quote.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.typingpractice.typing_practice_be.quote.config.DifficultyProperties;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteProfile;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.quote.service.difficulty.DifficultyScale;
import com.typingpractice.typing_practice_be.quote.service.difficulty.DifficultySeedCalculator;
import com.typingpractice.typing_practice_be.quote.service.difficulty.QuoteProfileCalculator;
import com.typingpractice.typing_practice_be.quote.statistics.domain.GlobalQuoteStatistics;
import com.typingpractice.typing_practice_be.quote.statistics.dto.QuoteProfileAggregation;
import com.typingpractice.typing_practice_be.quote.statistics.repository.GlobalQuoteStatisticsRepository;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.GlobalTypingPerformance;
import com.typingpractice.typing_practice_be.typingrecord.statistics.dto.PerfDistribution;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.QuoteTypingStatsRepository;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalQuoteStatisticsBatchServiceTest {
  @Mock private GlobalQuoteStatisticsRepository statsRepository;
  @Mock private GlobalQuoteStatisticsService statsService;
  @Mock private QuoteRepository quoteRepository;
  @Mock private QuoteTypingStatsRepository quoteTypingStatsRepository;

  private GlobalQuoteStatisticsBatchService batchService;

  private static final int K = 20;

  @BeforeEach
  void setUp() {
    // 계산기는 순수 로직이라 실제 객체를 쓴다
    batchService =
        new GlobalQuoteStatisticsBatchService(
            statsRepository,
            statsService,
            quoteRepository,
            new DifficultySeedCalculator(),
            new QuoteProfileCalculator(),
            quoteTypingStatsRepository,
            new DifficultyProperties(K));
  }

  // createKoreanDefault / createEnglishDefault 와 같은 평균·σ → 5% 임계값을 넘지 않는 집계
  private QuoteProfileAggregation koreanAggregation() {
    return QuoteProfileAggregation.from(
        new Object[] {
          30f, 10f, 0.05f, 0.03f, 0.18f, 0.05f, 0.0005f, 0.006f, 0.5f, 0.2f, 0.08f, 0.05f, 0.05f,
          0.03f, 0f, 0f, 0f, 0f
        });
  }

  private QuoteProfileAggregation englishAggregation() {
    return QuoteProfileAggregation.from(
        new Object[] {
          40f, 15f, 0.04f, 0.02f, 0.18f, 0.04f, 0.01f, 0.02f, 0f, 0f, 0f, 0f, 0f, 0f, 0.05f, 0.03f,
          4.5f, 1.5f
        });
  }

  /** 길이 외 모든 특성이 한국어 기본 평균인 공개·활성 문장. rawSeed = 0.2 × (length − 30)/10 */
  private Quote publicQuote(long id, int length) {
    QuoteProfile profile = QuoteProfile.create();
    profile.setLength(length);
    profile.setPuncRate(0.05f);
    profile.setSpaceRate(0.18f);
    profile.setDigitRate(0.0005f);
    profile.setJamoComplex(0.5f);
    profile.setDiphthongRate(0.08f);
    profile.setShiftJamoRate(0.05f);
    Quote quote =
        Quote.create(
            null, "문장 " + id, "저자", QuoteType.PUBLIC, QuoteLanguage.KOREAN, profile, 50f, "hash" + id);
    quote.approvePublish();
    setId(quote, id);
    return quote;
  }

  private void setId(Object entity, Long id) {
    try {
      Field idField = entity.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void stubCommonAggregations(PerfDistribution koreanPerf) {
    when(statsRepository.aggregateByLanguage(QuoteLanguage.KOREAN)).thenReturn(koreanAggregation());
    when(statsRepository.aggregateByLanguage(QuoteLanguage.ENGLISH)).thenReturn(englishAggregation());
    when(quoteTypingStatsRepository.aggregateGlobalAvgByLanguage(QuoteLanguage.KOREAN))
        .thenReturn(GlobalTypingPerformance.of(300f, 0.95f));
    when(quoteTypingStatsRepository.aggregateGlobalAvgByLanguage(QuoteLanguage.ENGLISH))
        .thenReturn(GlobalTypingPerformance.empty());
    when(quoteTypingStatsRepository.aggregatePerfDistributionByLanguage(
            QuoteLanguage.KOREAN, 300f, 0.95f, K))
        .thenReturn(koreanPerf);
  }

  private GlobalQuoteStatistics savedKorean() {
    ArgumentCaptor<GlobalQuoteStatistics> captor =
        ArgumentCaptor.forClass(GlobalQuoteStatistics.class);
    verify(statsRepository, times(2)).save(captor.capture());
    return captor.getAllValues().stream()
        .filter(s -> s.getLanguage() == QuoteLanguage.KOREAN)
        .findFirst()
        .orElseThrow();
  }

  @Test
  @DisplayName("수동 재계산: 1차 패스로 seedMean/seedStd 를 구해 저장하고, 2차 패스가 그 값으로 seed 를 쓴다")
  void manualRecalculationTwoPass() {
    // given
    stubCommonAggregations(PerfDistribution.of(500, 0.001f, 0.08f));
    List<Quote> quotes = List.of(publicQuote(1, 20), publicQuote(2, 30), publicQuote(3, 40));
    // rawSeed = -0.2, 0, +0.2 → mean 0, std sqrt(0.08/3) = 0.1633
    when(quoteRepository.findMaxIdByLanguage(QuoteLanguage.KOREAN)).thenReturn(3L);
    when(quoteRepository.findMaxIdByLanguage(QuoteLanguage.ENGLISH)).thenReturn(null);
    when(quoteRepository.findPageByLanguageAndIdRange(
            eq(QuoteLanguage.KOREAN), eq(0L), eq(3L), anyInt()))
        .thenReturn(quotes);
    when(quoteRepository.findPageByLanguageAndIdRange(
            eq(QuoteLanguage.KOREAN), eq(3L), eq(3L), anyInt()))
        .thenReturn(List.of());

    // when
    batchService.runManualRecalculation();

    // then
    GlobalQuoteStatistics korean = savedKorean();
    assertThat(korean.getSeedMeanOrDefault()).isCloseTo(0f, within(1e-4f));
    assertThat(korean.getSeedStdOrDefault()).isCloseTo(0.1633f, within(1e-3f));
    assertThat(korean.getPerfMeanOrDefault()).isEqualTo(0.001f);
    assertThat(korean.getPerfStdOrDefault()).isEqualTo(0.08f);

    // 평균 문장 50, ±1σ 길이는 표준화돼 50 에서 멀어진다 (raw 0.2 / 0.1633 = z 1.22 → 74)
    assertThat(quotes.get(1).getProfile().getDifficultySeed()).isEqualTo(50f);
    assertThat(quotes.get(2).getProfile().getDifficultySeed()).isGreaterThanOrEqualTo(70f);
    assertThat(quotes.get(0).getProfile().getDifficultySeed()).isLessThanOrEqualTo(30f);
    assertThat(quotes.get(2).getDifficulty()).isEqualTo(quotes.get(2).getProfile().getDifficultySeed());

    // 영어는 타이핑 기록이 없어 perf 분포 집계를 호출하지 않는다
    verify(quoteTypingStatsRepository, never())
        .aggregatePerfDistributionByLanguage(eq(QuoteLanguage.ENGLISH), anyFloat(), anyFloat(), anyInt());
    verify(statsService).refreshCache();
  }

  @Test
  @DisplayName("스케줄 배치: 특성 평균 변화가 임계값 미달이면 seed 를 재계산하지 않고 전날 seed 통계를 복사한다")
  void scheduledBatchCopiesSeedDistributionWhenSkipped() {
    // given
    GlobalQuoteStatistics prevKorean = GlobalQuoteStatistics.createKoreanDefault();
    prevKorean.updateSeedDistribution(0.01f, 0.2f);
    GlobalQuoteStatistics prevEnglish = GlobalQuoteStatistics.createEnglishDefault();
    prevEnglish.updateSeedDistribution(0f, 0.3f); // 양쪽 모두 seed 통계가 있어 재계산 조건은 임계값만 남는다
    when(statsService.getByLanguage(QuoteLanguage.KOREAN)).thenReturn(prevKorean);
    when(statsService.getByLanguage(QuoteLanguage.ENGLISH)).thenReturn(prevEnglish);
    stubCommonAggregations(PerfDistribution.of(500, 0f, 0.08f));

    // when
    batchService.runScheduledBatch();

    // then
    GlobalQuoteStatistics korean = savedKorean();
    assertThat(korean.getSeedMeanOrDefault()).isEqualTo(0.01f);
    assertThat(korean.getSeedStdOrDefault()).isEqualTo(0.2f);
    assertThat(korean.getPerfStdOrDefault()).isEqualTo(0.08f);
    verify(quoteRepository, never()).findMaxIdByLanguage(any());
  }

  @Test
  @DisplayName("수동 재계산: 모든 문장의 rawSeed 가 같아 분산이 0 이면 seed 통계를 저장하지 않고 seed 는 50")
  void zeroVarianceKeepsSeedDefault() {
    // given
    stubCommonAggregations(PerfDistribution.of(500, 0f, 0.08f));
    List<Quote> quotes = List.of(publicQuote(1, 30), publicQuote(2, 30)); // rawSeed 모두 0
    when(quoteRepository.findMaxIdByLanguage(QuoteLanguage.KOREAN)).thenReturn(2L);
    when(quoteRepository.findMaxIdByLanguage(QuoteLanguage.ENGLISH)).thenReturn(null);
    when(quoteRepository.findPageByLanguageAndIdRange(
            eq(QuoteLanguage.KOREAN), eq(0L), eq(2L), anyInt()))
        .thenReturn(quotes);
    when(quoteRepository.findPageByLanguageAndIdRange(
            eq(QuoteLanguage.KOREAN), eq(2L), eq(2L), anyInt()))
        .thenReturn(List.of());

    // when
    batchService.runManualRecalculation();

    // then
    GlobalQuoteStatistics korean = savedKorean();
    assertThat(korean.getSeedStdOrDefault()).isEqualTo(DifficultyScale.DEFAULT_SEED_STD);
    assertThat(quotes.get(0).getProfile().getDifficultySeed()).isEqualTo(50f);
    assertThat(quotes.get(1).getProfile().getDifficultySeed()).isEqualTo(50f);
  }

  @Test
  @DisplayName("perf 표본이 2개 미만이면 perf 통계를 저장하지 않아 기본값이 유지된다")
  void perfDistributionNotUsableKeepsDefault() {
    // given
    when(statsService.getByLanguage(QuoteLanguage.KOREAN))
        .thenReturn(GlobalQuoteStatistics.createKoreanDefault());
    when(statsService.getByLanguage(QuoteLanguage.ENGLISH))
        .thenReturn(GlobalQuoteStatistics.createEnglishDefault());
    stubCommonAggregations(PerfDistribution.of(1, 0f, 0f));

    // when
    batchService.runScheduledBatch();

    // then
    GlobalQuoteStatistics korean = savedKorean();
    assertThat(korean.getPerfMeanOrDefault()).isEqualTo(DifficultyScale.DEFAULT_PERF_MEAN);
    assertThat(korean.getPerfStdOrDefault()).isEqualTo(DifficultyScale.DEFAULT_PERF_STD);
  }

  @Test
  @DisplayName("스케줄 배치: 임계값 미달이라도 전날 행에 seed 통계가 없으면 seed 를 재계산해 채운다 (수동 재계산 누락 자가 복구)")
  void scheduledBatchRecalculatesWhenPrevHasNoSeedDistribution() {
    // given — 마이그레이션 직후처럼 전날 행의 seedMean/seedStd 가 null
    when(statsService.getByLanguage(QuoteLanguage.KOREAN))
        .thenReturn(GlobalQuoteStatistics.createKoreanDefault());
    when(statsService.getByLanguage(QuoteLanguage.ENGLISH))
        .thenReturn(GlobalQuoteStatistics.createEnglishDefault());
    stubCommonAggregations(PerfDistribution.of(500, 0f, 0.08f));
    List<Quote> quotes = List.of(publicQuote(1, 20), publicQuote(2, 30), publicQuote(3, 40));
    when(quoteRepository.findMaxIdByLanguage(QuoteLanguage.KOREAN)).thenReturn(3L);
    when(quoteRepository.findMaxIdByLanguage(QuoteLanguage.ENGLISH)).thenReturn(null);
    when(quoteRepository.findPageByLanguageAndIdRange(
            eq(QuoteLanguage.KOREAN), eq(0L), eq(3L), anyInt()))
        .thenReturn(quotes);
    when(quoteRepository.findPageByLanguageAndIdRange(
            eq(QuoteLanguage.KOREAN), eq(3L), eq(3L), anyInt()))
        .thenReturn(List.of());

    // when
    batchService.runScheduledBatch();

    // then — 특성 평균은 변하지 않았지만 seed 통계가 비어 있어 재계산된다
    GlobalQuoteStatistics korean = savedKorean();
    assertThat(korean.getSeedStdOrDefault()).isCloseTo(0.1633f, within(1e-3f));
    assertThat(quotes.get(2).getProfile().getDifficultySeed()).isGreaterThanOrEqualTo(70f);
  }
}
