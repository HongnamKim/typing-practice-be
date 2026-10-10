package com.typingpractice.typing_practice_be.common.utils;

// 오늘 오타 상세 Redis 키 "{언어}:{정답}:{입력}" 해석 (문장·단어 모드 공통)
public class TypoDetailKeyUtils {

  // 키를 [정답, 입력] 으로 나눈다. ':' 를 친 오타도 있으므로 정답은 최소 한 글자로 보고 그 뒤 첫 ':' 를 구분자로 쓴다.
  // 정답이 빈 값이면 첫 ':' 가 구분자다.
  public static String[] splitExpectedAndActual(Enum<?> language, String key) {
    String expectedAndActual = key.substring((language + ":").length());
    int firstCharEnd = expectedAndActual.offsetByCodePoints(0, 1);
    int separator = expectedAndActual.indexOf(':', firstCharEnd);
    if (separator < 0) {
      separator = expectedAndActual.indexOf(':');
    }
    return new String[] {
      expectedAndActual.substring(0, separator), expectedAndActual.substring(separator + 1)
    };
  }
}
