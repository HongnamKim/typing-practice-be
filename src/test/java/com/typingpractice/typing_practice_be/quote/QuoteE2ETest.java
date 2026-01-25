package com.typingpractice.typing_practice_be.quote;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.auth.dto.LoginResponse;
import com.typingpractice.typing_practice_be.auth.dto.TestLoginRequest;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuotePaginationResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class QuoteE2ETest {
  @Autowired private TestRestTemplate restTemplate;

  private static final String USER_PROVIDER_ID = "1";

  private String getAccessToken(String providerId) {
    TestLoginRequest request = TestLoginRequest.create(providerId);
    ResponseEntity<ApiResponse<LoginResponse>> response =
        restTemplate.exchange(
            "/auth/test",
            HttpMethod.POST,
            new HttpEntity<>(request),
            new ParameterizedTypeReference<>() {});

    assert response.getBody() != null;
    return response.getBody().data().getAccessToken();
  }

  private HttpHeaders createAuthHeader(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  @Nested
  @DisplayName("GET /quotes - 공개 문장 랜덤 조회")
  class GetPublicQuotes {
    @Test
    @DisplayName("성공")
    void success() {
      ResponseEntity<ApiResponse<QuotePaginationResponse>> response =
          restTemplate.exchange(
              "/quotes?seed=0.5", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().success()).isTrue();
      assertThat(response.getBody().data().getContent()).isNotNull();
    }

    @Test
    @DisplayName("seed가 같으면 동일한 순서로 반환")
    void sameSeedSameOrder() {
      ResponseEntity<ApiResponse<QuotePaginationResponse>> response1 =
          restTemplate.exchange(
              "/quotes?seed=0.5", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      ResponseEntity<ApiResponse<QuotePaginationResponse>> response2 =
          restTemplate.exchange(
              "/quotes?seed=0.5", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      List<Long> quoteIds1 =
          response1.getBody().data().getContent().stream().map(QuoteResponse::getQuoteId).toList();
      List<Long> quoteIds2 =
          response2.getBody().data().getContent().stream().map(QuoteResponse::getQuoteId).toList();

      assertThat(quoteIds1).isEqualTo(quoteIds2);
    }

    @Test
    @DisplayName("validation 실패 시 400")
    void badRequestWhenInvalid() {
      // seed 없음
      assertThat(
              restTemplate
                  .exchange(
                      "/quotes",
                      HttpMethod.GET,
                      null,
                      new ParameterizedTypeReference<ApiResponse<QuotePaginationResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);

      // seed 범위 초과
      assertThat(
              restTemplate
                  .exchange(
                      "/quotes?seed=2.0",
                      HttpMethod.GET,
                      null,
                      new ParameterizedTypeReference<ApiResponse<QuotePaginationResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);

      // count 범위 미달
      assertThat(
              restTemplate
                  .exchange(
                      "/quotes?seed=0.5&count=50",
                      HttpMethod.GET,
                      null,
                      new ParameterizedTypeReference<ApiResponse<QuotePaginationResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);

      // count 범위 초과
      assertThat(
              restTemplate
                  .exchange(
                      "/quotes?seed=0.5&count=500",
                      HttpMethod.GET,
                      null,
                      new ParameterizedTypeReference<ApiResponse<QuotePaginationResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);
    }
  }

  @Nested
  @DisplayName("GET /quotes/my - 내 문장 조회")
  class GetMyQuotes {
    @Test
    @DisplayName("성공")
    void success() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuotePaginationResponse>> response =
          restTemplate.exchange(
              "/quotes/my",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().success()).isTrue();
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<Map> response =
          restTemplate.exchange("/quotes/my", HttpMethod.GET, null, Map.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /quotes/{quoteId - 문장 상세 조회")
  class GetQuoteById {
    @Test
    @DisplayName("성공")
    void success() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/33",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().data().getQuoteId()).isEqualTo(33L);
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 404")
    void notFound() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuoteResponse>> exchange =
          restTemplate.exchange(
              "/quotes/81234",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/33", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /quotes/public - 공개 문장 업로드")
  class CreatePublicQuote {
    @Test
    @DisplayName("성공")
    void success() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);
      QuoteCreateRequest request = QuoteCreateRequest.create("테스트 공개 문장입니다.", "저자");

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/public",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().data().getType().name()).isEqualTo("PUBLIC");
      assertThat(response.getBody().data().getStatus().name()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("validation 실패 시 400")
    void badRequestWhenInvalid() {
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      // sentence 5자 미만
      assertThat(
              restTemplate
                  .exchange(
                      "/quotes/public",
                      HttpMethod.POST,
                      new HttpEntity<>(QuoteCreateRequest.create("1234", null), headers),
                      new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);

      // sentence 100자 초과
      assertThat(
              restTemplate
                  .exchange(
                      "/quotes/public",
                      HttpMethod.POST,
                      new HttpEntity<>(QuoteCreateRequest.create("a".repeat(101), null), headers),
                      new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);

      // author 20자 초과
      assertThat(
              restTemplate
                  .exchange(
                      "/quotes/public",
                      HttpMethod.POST,
                      new HttpEntity<>(
                          QuoteCreateRequest.create("테스트 문장입니다.", "a".repeat(21)), headers),
                      new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      QuoteCreateRequest request = QuoteCreateRequest.create("테스트 공개 문장입니다.", null);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/public",
              HttpMethod.POST,
              new HttpEntity<>(request),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /quotes/private - 비공개 문장 업로드")
  class CreatePrivateQuote {
    @Test
    @DisplayName("성공")
    void success() {
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);
      QuoteCreateRequest request = QuoteCreateRequest.create("테스트 비공개 문장입니다.", null);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/private",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().data().getType().name()).isEqualTo("PRIVATE");
      assertThat(response.getBody().data().getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      QuoteCreateRequest request = QuoteCreateRequest.create("테스트 비공개 문장입니다.", null);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/private",
              HttpMethod.POST,
              new HttpEntity<>(request),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("PATCH /quotes/{quoteId} - 비공개 문장 수정")
  class UpdatePrivateQuote {

    @Test
    @DisplayName("성공")
    void success() {
      // 비공개 문장 생성
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      QuoteCreateRequest createRequest = QuoteCreateRequest.create("수정 테스트용 문장입니다.", null);
      ResponseEntity<ApiResponse<QuoteResponse>> createResponse =
          restTemplate.exchange(
              "/quotes/private",
              HttpMethod.POST,
              new HttpEntity<>(createRequest, headers),
              new ParameterizedTypeReference<>() {});
      assert createResponse.getBody() != null;
      Long quoteId = createResponse.getBody().data().getQuoteId();

      // 수정
      QuoteUpdateRequest updateRequest = QuoteUpdateRequest.create("수정된 문장입니다.", "수정된작성자");
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/" + quoteId,
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getSentence()).isEqualTo("수정된 문장입니다.");
      assertThat(response.getBody().data().getAuthor()).isEqualTo("수정된작성자");
    }

    @Test
    @DisplayName("validation 실패 시 400")
    void badRequestWhenInvalid() {
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      // sentence, author 둘 다 없음
      assertThat(
              restTemplate
                  .exchange(
                      "/quotes/5",
                      HttpMethod.PATCH,
                      new HttpEntity<>(QuoteUpdateRequest.create(null, null), headers),
                      new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      QuoteUpdateRequest request = QuoteUpdateRequest.create("수정된 문장입니다.", null);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/5",
              HttpMethod.PATCH,
              new HttpEntity<>(request),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("DELETE /quotes/{quoteId} - 비공개 문장 삭제")
  class DeleteQuote {

    @Test
    @DisplayName("성공")
    void success() {
      // 비공개 문장 생성
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      QuoteCreateRequest createRequest = QuoteCreateRequest.create("삭제 테스트용 문장입니다.", null);
      ResponseEntity<ApiResponse<QuoteResponse>> createResponse =
          restTemplate.exchange(
              "/quotes/private",
              HttpMethod.POST,
              new HttpEntity<>(createRequest, headers),
              new ParameterizedTypeReference<>() {});
      Long quoteId = createResponse.getBody().data().getQuoteId();

      // 삭제
      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/quotes/" + quoteId,
              HttpMethod.DELETE,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/quotes/5", HttpMethod.DELETE, null, new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /quotes/{quoteId}/publish - 개인용 문장 공개 전환")
  class PublishQuote {

    @Test
    @DisplayName("성공")
    void success() {
      // 비공개 문장 생성
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      QuoteCreateRequest createRequest = QuoteCreateRequest.create("공개 전환 테스트용 문장입니다.", null);
      ResponseEntity<ApiResponse<QuoteResponse>> createResponse =
          restTemplate.exchange(
              "/quotes/private",
              HttpMethod.POST,
              new HttpEntity<>(createRequest, headers),
              new ParameterizedTypeReference<>() {});
      Long quoteId = createResponse.getBody().data().getQuoteId();

      // 공개 전환
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/" + quoteId + "/publish",
              HttpMethod.POST,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().data().getType().name()).isEqualTo("PUBLIC");
      assertThat(response.getBody().data().getStatus().name()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/5/publish", HttpMethod.POST, null, new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /quotes/{quoteId}/cancel-publish - 공개 전환 취소")
  class CancelPublishQuote {

    @Test
    @DisplayName("성공")
    void success() {
      // 비공개 문장 생성 후 공개 전환
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      QuoteCreateRequest createRequest = QuoteCreateRequest.create("공개 취소 테스트용 문장입니다.", null);
      ResponseEntity<ApiResponse<QuoteResponse>> createResponse =
          restTemplate.exchange(
              "/quotes/private",
              HttpMethod.POST,
              new HttpEntity<>(createRequest, headers),
              new ParameterizedTypeReference<>() {});
      Long quoteId = createResponse.getBody().data().getQuoteId();

      // 공개 전환
      restTemplate.exchange(
          "/quotes/" + quoteId + "/publish",
          HttpMethod.POST,
          new HttpEntity<>(headers),
          new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {});

      // 공개 취소
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/" + quoteId + "/cancel-publish",
              HttpMethod.POST,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().data().getType().name()).isEqualTo("PRIVATE");
      assertThat(response.getBody().data().getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/quotes/5/cancel-publish",
              HttpMethod.POST,
              null,
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }
}
