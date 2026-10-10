package com.typingpractice.typing_practice_be.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.typingpractice.typing_practice_be.BaseE2ETest;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.dto.response.MemberTypoDetailAllResponse;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.dto.response.MemberWordTypoDetailAllResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 키보드 히트맵용 전체 오타 상세 조회 */
public class TypoDetailAllE2ETest extends BaseE2ETest {

  private static final ParameterizedTypeReference<ApiResponse<MemberTypoDetailAllResponse>>
      QUOTE_RESPONSE = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<MemberWordTypoDetailAllResponse>>
      WORD_RESPONSE = new ParameterizedTypeReference<>() {};

  @Test
  @DisplayName("GET /members/me/stats/typos/detail/all - 요청 언어와 전체 오타 목록을 반환한다")
  void quoteTypoDetailAll() {
    HttpHeaders headers = createAuthHeader(getAccessToken(USER_PROVIDER_ID));

    ResponseEntity<ApiResponse<MemberTypoDetailAllResponse>> response =
        restTemplate.exchange(
            "/members/me/stats/typos/detail/all?language=KOREAN",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            QUOTE_RESPONSE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assert response.getBody() != null;
    MemberTypoDetailAllResponse.Content content = response.getBody().data().getContent();
    assertThat(content.getLanguage()).isEqualTo(QuoteLanguage.KOREAN);
    assertThat(content.getTypos()).isNotNull();
  }

  @Test
  @DisplayName("GET /members/me/word-stats/typos/detail/all - 요청 언어와 전체 오타 목록을 반환한다")
  void wordTypoDetailAll() {
    HttpHeaders headers = createAuthHeader(getAccessToken(USER_PROVIDER_ID));

    ResponseEntity<ApiResponse<MemberWordTypoDetailAllResponse>> response =
        restTemplate.exchange(
            "/members/me/word-stats/typos/detail/all?language=KOREAN",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            WORD_RESPONSE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assert response.getBody() != null;
    MemberWordTypoDetailAllResponse.Content content = response.getBody().data().getContent();
    assertThat(content.getLanguage()).isEqualTo(WordLanguage.KOREAN);
    assertThat(content.getTypos()).isNotNull();
  }
}
