package com.typingpractice.typing_practice_be.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.typingpractice.typing_practice_be.BaseE2ETest;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoStatsAllResponse;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.response.MemberWordTypoStatsAllResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 키보드 히트맵용 글자별 오타 횟수 전체 조회 */
public class TypoStatsAllE2ETest extends BaseE2ETest {

  private static final ParameterizedTypeReference<ApiResponse<MemberTypoStatsAllResponse>>
      QUOTE_RESPONSE = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<MemberWordTypoStatsAllResponse>>
      WORD_RESPONSE = new ParameterizedTypeReference<>() {};

  @Test
  @DisplayName("GET /members/me/stats/typos/all - 요청 언어와 글자별 오타 횟수를 반환한다")
  void quoteTypoStatsAll() {
    HttpHeaders headers = createAuthHeader(getAccessToken(USER_PROVIDER_ID));

    ResponseEntity<ApiResponse<MemberTypoStatsAllResponse>> response =
        restTemplate.exchange(
            "/members/me/stats/typos/all?language=KOREAN",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            QUOTE_RESPONSE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assert response.getBody() != null;
    MemberTypoStatsAllResponse.Content content = response.getBody().data().getContent();
    assertThat(content.getLanguage()).isEqualTo(QuoteLanguage.KOREAN);
    assertThat(content.getTypos()).isNotNull();
  }

  @Test
  @DisplayName("GET /members/me/word-stats/typos/all - 요청 언어와 글자별 오타 횟수를 반환한다")
  void wordTypoStatsAll() {
    HttpHeaders headers = createAuthHeader(getAccessToken(USER_PROVIDER_ID));

    ResponseEntity<ApiResponse<MemberWordTypoStatsAllResponse>> response =
        restTemplate.exchange(
            "/members/me/word-stats/typos/all?language=KOREAN",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            WORD_RESPONSE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assert response.getBody() != null;
    MemberWordTypoStatsAllResponse.Content content = response.getBody().data().getContent();
    assertThat(content.getLanguage()).isEqualTo(WordLanguage.KOREAN);
    assertThat(content.getTypos()).isNotNull();
  }
}
