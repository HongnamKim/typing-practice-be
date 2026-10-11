package com.typingpractice.typing_practice_be.adaptiveserving.service;

import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveQuoteResponse;
import com.typingpractice.typing_practice_be.adaptiveserving.dto.AdaptiveServingEstimation;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.dto.QuoteIdWithDifficulty;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.quote.service.QuoteIdCacheService;
import com.typingpractice.typing_practice_be.typingrecord.domain.ServingType;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdaptiveQuoteService {
  private final AdaptiveServingRedisService redisService;
  private final QuoteIdCacheService quoteIdCacheService;
  private final QuoteRepository quoteRepository;

  private static final int EASY_RATIO = 1;
  private static final int FIT_RATIO = 7;
  private static final int HARD_RATIO = 2;
  // 적정 구간 = 난이도순 정렬에서 mu 의 백분위를 중심으로 전체 공개 문장의 40%
  private static final float FIT_RATIO_OF_CORPUS = 0.4f;
  private static final float RANDOM_RATIO = 0.05f;

  public List<AdaptiveQuoteResponse> getAdaptiveQuotes(
      Long memberId, QuoteLanguage language, int count, List<Long> excludeIds) {
    AdaptiveServingEstimation estimation = redisService.getEstimation(memberId, language);
    float mu = estimation.getMu();

    List<QuoteIdWithDifficulty> allQuotes = quoteIdCacheService.getPublicQuotes(language);

    // 난이도순 정렬 후 mu 의 백분위 기준으로 적정 구간 [fitStart, fitEnd) 산출
    List<QuoteIdWithDifficulty> sorted = new ArrayList<>(allQuotes);
    sorted.sort(Comparator.comparingDouble(QuoteIdWithDifficulty::getDifficulty));
    int n = sorted.size();
    int rank = 0;
    while (rank < n && sorted.get(rank).getDifficulty() < mu) rank++;
    float half = FIT_RATIO_OF_CORPUS / 2;
    float percentile = n == 0 ? 0.5f : (float) rank / n;
    percentile = Math.max(half, Math.min(1 - half, percentile)); // 구간이 코퍼스 밖으로 나가지 않도록 클램프
    int fitStart = Math.round(n * (percentile - half));
    int fitEnd = Math.round(n * (percentile + half));

    // 이미 친 문장 제외
    Set<Long> excludeSet = excludeIds != null ? new HashSet<>(excludeIds) : Set.of();

    List<QuoteIdWithDifficulty> easyPool = new ArrayList<>();
    List<QuoteIdWithDifficulty> fitPool = new ArrayList<>();
    List<QuoteIdWithDifficulty> hardPool = new ArrayList<>();

    fillPools(sorted, fitStart, fitEnd, excludeSet, easyPool, fitPool, hardPool);

    Set<Long> picked = new HashSet<>(excludeSet);

    int totalAvailable = easyPool.size() + fitPool.size() + hardPool.size();
    if (totalAvailable < count) {
      easyPool.clear();
      fitPool.clear();
      hardPool.clear();
      picked.clear();
      fillPools(sorted, fitStart, fitEnd, Set.of(), easyPool, fitPool, hardPool);
    }

    // RANDOM / ADAPTIVE 슬롯 수 계산
    int randomCount = Math.max(1, Math.round(count * RANDOM_RATIO));
    int adaptiveCount = count - randomCount;

    int totalRatio = EASY_RATIO + FIT_RATIO + HARD_RATIO;
    int easyCount = Math.round((float) adaptiveCount * EASY_RATIO / totalRatio);
    int hardCount = Math.round((float) adaptiveCount * HARD_RATIO / totalRatio);
    int fitCount = adaptiveCount - easyCount - hardCount;

    // 비율에 맞게 선택
    Random random = new Random();
    List<Long> adaptiveIds = new ArrayList<>();
    adaptiveIds.addAll(pickRandomIds(easyPool, easyCount, random, picked));
    adaptiveIds.addAll(pickRandomIds(fitPool, fitCount, random, picked));
    adaptiveIds.addAll(pickRandomIds(hardPool, hardCount, random, picked));

    // 부족분은 random 으로 보충
    randomCount += (adaptiveCount - adaptiveIds.size());

    List<Long> randomSelectedIds = pickRandomIds(allQuotes, randomCount, random, picked);
    Set<Long> randomIdSet = new HashSet<>(randomSelectedIds);

    // 전체 ID
    List<Long> allIds = new ArrayList<>(adaptiveIds);
    allIds.addAll(randomSelectedIds);

    // DB fetch → 셔플 → 응답
    List<Quote> fetched = quoteRepository.findByIds(allIds, language);
    Collections.shuffle(fetched, random);

    return fetched.stream()
        .map(
            q ->
                AdaptiveQuoteResponse.from(
                    q, randomIdSet.contains(q.getId()) ? ServingType.RANDOM : ServingType.ADAPTIVE))
        .toList();
  }

  /** 정렬된 문장을 적정 구간 인덱스 기준으로 easy / fit / hard 풀에 나눠 담는다 (exclude 제외) */
  private void fillPools(
      List<QuoteIdWithDifficulty> sorted,
      int fitStart,
      int fitEnd,
      Set<Long> exclude,
      List<QuoteIdWithDifficulty> easyPool,
      List<QuoteIdWithDifficulty> fitPool,
      List<QuoteIdWithDifficulty> hardPool) {
    for (int i = 0; i < sorted.size(); i++) {
      QuoteIdWithDifficulty q = sorted.get(i);
      if (exclude.contains(q.getId())) continue;

      if (i < fitStart) easyPool.add(q);
      else if (i >= fitEnd) hardPool.add(q);
      else fitPool.add(q);
    }
  }

  private List<Long> pickRandomIds(
      List<QuoteIdWithDifficulty> pool, int count, Random random, Set<Long> picked) {
    if (pool.isEmpty() || count <= 0) return new ArrayList<>();

    List<Long> result = new ArrayList<>();
    List<QuoteIdWithDifficulty> candidates = new ArrayList<>(pool);

    while (result.size() < count && !candidates.isEmpty()) {
      int idx = random.nextInt(candidates.size());
      QuoteIdWithDifficulty q = candidates.remove(idx);
      if (!picked.contains(q.getId())) {
        result.add(q.getId());
        picked.add(q.getId());
      }
    }

    return result;
  }
}
