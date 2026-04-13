package com.typingpractice.typing_practice_be.adaptiveserving.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdaptiveServingRecord {
  private int cpm;
  private float accuracy;
  private float quoteDifficulty;
  private float avgCpmSnapshot;
  private float avgAccSnapshot;

  public static AdaptiveServingRecord create(
      int cpm, float accuracy, float quoteDifficulty, float avgCpmSnapshot, float avgAccSnapshot) {
    AdaptiveServingRecord record = new AdaptiveServingRecord();

    record.cpm = cpm;
    record.accuracy = accuracy;
    record.quoteDifficulty = quoteDifficulty;
    record.avgCpmSnapshot = avgCpmSnapshot;
    record.avgAccSnapshot = avgAccSnapshot;

    return record;
  }
}
