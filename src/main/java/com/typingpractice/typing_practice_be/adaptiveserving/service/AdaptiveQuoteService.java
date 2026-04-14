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

  private static final int EASY_RATIO = 2;
  private static final int FIT_RATIO = 5;
  private static final int HARD_RATIO = 3;
  private static final float MIN_HALF_WIDTH = 3f;
  private static final float RANDOM_RATIO = 0.2f;

  public List<AdaptiveQuoteResponse> getAdaptiveQuotes(
      Long memberId, QuoteLanguage language, int count, List<Long> excludeIds) {
    AdaptiveServingEstimation estimation = redisService.getEstimation(memberId, language);
    float mu = estimation.getMu();
    float sigma = estimation.getSigma();

    List<QuoteIdWithDifficulty> allQuotes = quoteIdCacheService.getPublicQuotes(language);

    // 이미 친 문장 제외
    Set<Long> excludeSet = excludeIds != null ? new HashSet<>(excludeIds) : Set.of();
    float halfWidth = Math.max(0.5f * sigma, MIN_HALF_WIDTH);
    float easyMax = mu - halfWidth;
    float hardMin = mu + halfWidth;

    List<QuoteIdWithDifficulty> easyPool = new ArrayList<>();
    List<QuoteIdWithDifficulty> fitPool = new ArrayList<>();
    List<QuoteIdWithDifficulty> hardPool = new ArrayList<>();

    for (QuoteIdWithDifficulty q : allQuotes) {
      if (excludeSet.contains(q.getId())) continue;

      float d = q.getDifficulty();
      if (d < easyMax) easyPool.add(q);
      else if (d > hardMin) hardPool.add(q);
      else fitPool.add(q);
    }

    Set<Long> picked = new HashSet<>(excludeSet);

    int totalAvailable = easyPool.size() + fitPool.size() + hardPool.size();
    if (totalAvailable < count) {
      easyPool.clear();
      fitPool.clear();
      hardPool.clear();
      picked.clear();
      for (QuoteIdWithDifficulty q : allQuotes) {
        float d = q.getDifficulty();
        if (d < easyMax) easyPool.add(q);
        else if (d > hardMin) hardPool.add(q);
        else fitPool.add(q);
      }
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
