package com.typingpractice.typing_practice_be.adaptiveserving.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveQuoteResponse;
import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveServingEstimation;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.QuoteIdWithDifficulty;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.quote.service.QuoteIdCacheService;
import com.typingpractice.typing_practice_be.typingrecord.domain.ServingType;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdaptiveQuoteServiceTest {
  @Mock private AdaptiveServingRedisService redisService;
  @Mock private QuoteIdCacheService quoteIdCacheService;
  @Mock private QuoteRepository quoteRepository;

  @InjectMocks private AdaptiveQuoteService adaptiveQuoteService;

  private static final Long MEMBER_ID = 1L;
  private static final QuoteLanguage LANG = QuoteLanguage.KOREAN;
  private static final int COUNT = 50;

  // count=50 기준 슬롯 수: RANDOM round(2.5)=3, ADAPTIVE 47 → easy 5 : fit 33 : hard 9
  private static final int RANDOM_SLOTS = 3;
  private static final int EASY_SLOTS = 5;
  private static final int FIT_SLOTS = 33;
  private static final int HARD_SLOTS = 9;

  /** id == difficulty 인 문장 100개 (1 ~ 100). 백분위 계산을 눈으로 검증하기 위한 코퍼스 */
  private List<QuoteIdWithDifficulty> corpus100() {
    return IntStream.rangeClosed(1, 100)
        .mapToObj(i -> QuoteIdWithDifficulty.create((long) i, (float) i))
        .toList();
  }

  private void givenCorpusAndEstimation(float mu, float sigma) {
    when(quoteIdCacheService.getPublicQuotes(LANG)).thenReturn(corpus100());
    when(redisService.getEstimation(MEMBER_ID, LANG))
        .thenReturn(AdaptiveServingEstimation.of(mu, sigma));
    when(quoteRepository.findByIds(anyList(), eq(LANG)))
        .thenAnswer(
            inv -> {
              List<Long> ids = inv.getArgument(0);
              return new java.util.ArrayList<>(ids.stream().map(this::quoteWithId).toList());
            });
  }

  private Quote quoteWithId(Long id) {
    Member member = Member.createMember("provider", "test@test.com", "tester");
    Quote quote =
        Quote.create(
            member, "문장 " + id, "저자", QuoteType.PUBLIC, LANG, null, (float) id, "hash" + id);
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

  private List<AdaptiveQuoteResponse> adaptive(List<AdaptiveQuoteResponse> result) {
    return result.stream().filter(q -> q.getServingType() == ServingType.ADAPTIVE).toList();
  }

  private long countInRange(List<AdaptiveQuoteResponse> quotes, long from, long to) {
    return quotes.stream().filter(q -> q.getQuoteId() >= from && q.getQuoteId() <= to).count();
  }

  @Nested
  @DisplayName("적정 구간은 μ 의 백분위 기준 전체의 40%")
  class PercentileBand {
    @Test
    @DisplayName("μ 가 중앙(50.5)이면 적정 구간은 순위 30~70% → id 31~70 에서 33개 선택")
    void centerMu() {
      // given
      givenCorpusAndEstimation(50.5f, 2f);

      // when
      List<AdaptiveQuoteResponse> result =
          adaptiveQuoteService.getAdaptiveQuotes(MEMBER_ID, LANG, COUNT, null);

      // then
      assertThat(result).hasSize(COUNT);
      List<AdaptiveQuoteResponse> adaptive = adaptive(result);
      assertThat(adaptive).hasSize(EASY_SLOTS + FIT_SLOTS + HARD_SLOTS);
      assertThat(countInRange(adaptive, 31, 70)).isEqualTo(FIT_SLOTS);
      assertThat(countInRange(adaptive, 1, 30)).isEqualTo(EASY_SLOTS);
      assertThat(countInRange(adaptive, 71, 100)).isEqualTo(HARD_SLOTS);
      assertThat(result.size() - adaptive.size()).isEqualTo(RANDOM_SLOTS);
    }

    @Test
    @DisplayName("μ 가 코퍼스 최대보다 크면 상위 40%(id 61~100)로 클램프, hard 부족분은 RANDOM 보충")
    void muAboveCorpus() {
      // given
      givenCorpusAndEstimation(120f, 2f);

      // when
      List<AdaptiveQuoteResponse> result =
          adaptiveQuoteService.getAdaptiveQuotes(MEMBER_ID, LANG, COUNT, null);

      // then
      assertThat(result).hasSize(COUNT);
      List<AdaptiveQuoteResponse> adaptive = adaptive(result);
      assertThat(countInRange(adaptive, 61, 100)).isEqualTo(FIT_SLOTS);
      assertThat(countInRange(adaptive, 1, 60)).isEqualTo(EASY_SLOTS);
      assertThat(adaptive).hasSize(EASY_SLOTS + FIT_SLOTS);
      assertThat(result.size() - adaptive.size()).isEqualTo(RANDOM_SLOTS + HARD_SLOTS);
    }

    @Test
    @DisplayName("μ 가 코퍼스 최소보다 작으면 하위 40%(id 1~40)로 클램프, easy 부족분은 RANDOM 보충")
    void muBelowCorpus() {
      // given
      givenCorpusAndEstimation(-10f, 2f);

      // when
      List<AdaptiveQuoteResponse> result =
          adaptiveQuoteService.getAdaptiveQuotes(MEMBER_ID, LANG, COUNT, null);

      // then
      assertThat(result).hasSize(COUNT);
      List<AdaptiveQuoteResponse> adaptive = adaptive(result);
      assertThat(countInRange(adaptive, 1, 40)).isEqualTo(FIT_SLOTS);
      assertThat(countInRange(adaptive, 41, 100)).isEqualTo(HARD_SLOTS);
      assertThat(adaptive).hasSize(FIT_SLOTS + HARD_SLOTS);
      assertThat(result.size() - adaptive.size()).isEqualTo(RANDOM_SLOTS + EASY_SLOTS);
    }
  }

  @Nested
  @DisplayName("excludeIds")
  class ExcludeIds {
    @Test
    @DisplayName("제외 문장은 반환되지 않고, 구간은 제외 전 전체 코퍼스 기준으로 유지된다")
    void excludedQuotesKeepBandFixed() {
      // given
      givenCorpusAndEstimation(50.5f, 2f);
      // 적정 구간(id 31~70) 중 31~60 을 제외 → 적정 풀에는 61~70 만 남음
      List<Long> excludeIds = IntStream.rangeClosed(31, 60).mapToObj(Long::valueOf).toList();
      Set<Long> excluded = Set.copyOf(excludeIds);

      // when
      List<AdaptiveQuoteResponse> result =
          adaptiveQuoteService.getAdaptiveQuotes(MEMBER_ID, LANG, COUNT, excludeIds);

      // then
      assertThat(result).hasSize(COUNT);
      assertThat(result).noneMatch(q -> excluded.contains(q.getQuoteId()));
      List<AdaptiveQuoteResponse> adaptive = adaptive(result);
      // 구간이 제외 후 재계산됐다면 61~70 밖에서도 fit 이 뽑혀 10개를 넘는다
      assertThat(countInRange(adaptive, 61, 70)).isEqualTo(10);
      assertThat(countInRange(adaptive, 1, 30)).isEqualTo(EASY_SLOTS);
      assertThat(countInRange(adaptive, 71, 100)).isEqualTo(HARD_SLOTS);
    }
  }
}
