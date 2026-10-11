package com.typingpractice.typing_practice_be.typingrecord.statistics.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 동적 보정 신호 perfRaw = cpmDrop + accDrop 의 코퍼스 분포 (설계서 4-3) */
@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerfDistribution {
  private long count;
  private float mean;
  private float std;

  public static PerfDistribution of(long count, float mean, float std) {
    PerfDistribution dist = new PerfDistribution();
    dist.count = count;
    dist.mean = mean;
    dist.std = std;
    return dist;
  }

  /** 표본 2개 미만이거나 분산 0 이면 표준화에 쓰지 않는다 */
  public boolean isUsable() {
    return count >= 2 && std > 0f;
  }
}
