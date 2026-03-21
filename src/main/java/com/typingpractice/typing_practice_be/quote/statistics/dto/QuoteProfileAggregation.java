package com.typingpractice.typing_practice_be.quote.statistics.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuoteProfileAggregation {
  // 공통
  private float lenMean;
  private float lenStd;
  private float puncMean;
  private float puncStd;
  private float spaceMean;
  private float spaceStd;
  private float digitMean;
  private float digitStd;

  // 한국어 전용
  private float jamoMean;
  private float jamoStd;
  private float diphthongMean;
  private float diphthongStd;
  private float shiftJamoMean;
  private float shiftJamoStd;

  // 영어 전용
  private float caseMean;
  private float caseStd;
  private float wordLenMean;
  private float wordLenStd;

  public static QuoteProfileAggregation from(Object[] row) {
    QuoteProfileAggregation agg = new QuoteProfileAggregation();
    agg.lenMean = toFloat(row[0]);
    agg.lenStd = toFloat(row[1]);
    agg.puncMean = toFloat(row[2]);
    agg.puncStd = toFloat(row[3]);
    agg.spaceMean = toFloat(row[4]);
    agg.spaceStd = toFloat(row[5]);
    agg.digitMean = toFloat(row[6]);
    agg.digitStd = toFloat(row[7]);
    agg.jamoMean = toFloat(row[8]);
    agg.jamoStd = toFloat(row[9]);
    agg.diphthongMean = toFloat(row[10]);
    agg.diphthongStd = toFloat(row[11]);
    agg.shiftJamoMean = toFloat(row[12]);
    agg.shiftJamoStd = toFloat(row[13]);
    agg.caseMean = toFloat(row[14]);
    agg.caseStd = toFloat(row[15]);
    agg.wordLenMean = toFloat(row[16]);
    agg.wordLenStd = toFloat(row[17]);
    return agg;
  }

  private static float toFloat(Object value) {
    if (value == null) return 0f;
    return ((Number) value).floatValue();
  }
}
