package com.typingpractice.typing_practice_be.word.statistics.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WordProfileAggregation {
  private float lenMean;
  private float lenStd;

  // 한국어
  private Float jamoMean;
  private Float jamoStd;
  private Float diphthongMean;
  private Float diphthongStd;
  private Float shiftJamoMean;
  private Float shiftJamoStd;

  // 영어
  private Float caseMean;
  private Float caseStd;

  public static WordProfileAggregation from(Object[] row) {
    WordProfileAggregation agg = new WordProfileAggregation();
    agg.lenMean = toFloat(row[0]);
    agg.lenStd = toFloat(row[1]);
    agg.jamoMean = toFloatOrNull(row[2]);
    agg.jamoStd = toFloatOrNull(row[3]);
    agg.diphthongMean = toFloatOrNull(row[4]);
    agg.diphthongStd = toFloatOrNull(row[5]);
    agg.shiftJamoMean = toFloatOrNull(row[6]);
    agg.shiftJamoStd = toFloatOrNull(row[7]);
    agg.caseMean = toFloatOrNull(row[8]);
    agg.caseStd = toFloatOrNull(row[9]);
    return agg;
  }

  private static float toFloat(Object value) {
    if (value == null) return 0f;
    return ((Number) value).floatValue();
  }

  private static Float toFloatOrNull(Object value) {
    if (value == null) return null;
    return ((Number) value).floatValue();
  }
}
