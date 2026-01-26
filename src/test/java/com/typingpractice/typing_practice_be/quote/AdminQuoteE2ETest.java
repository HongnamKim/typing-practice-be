package com.typingpractice.typing_practice_be.quote;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.auth.dto.LoginResponse;
import com.typingpractice.typing_practice_be.auth.dto.TestLoginRequest;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuotePaginationResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AdminQuoteE2ETest {
  @Autowired private TestRestTemplate restTemplate;

  private static final String ADMIN_PROVIDER_ID = "0";
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

  private Long createPublicQuote(String token) {
    HttpHeaders headers = createAuthHeader(token);
    QuoteCreateRequest request = QuoteCreateRequest.create("테스트용 문장입니다.", null);

    ResponseEntity<ApiResponse<QuoteResponse>> response =
        restTemplate.exchange(
            "/quotes/public",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            new ParameterizedTypeReference<>() {});

    assert response.getBody() != null;
    return response.getBody().data().getQuoteId();
  }

  private Long createPrivateQuote(String token) {
    HttpHeaders headers = createAuthHeader(token);
    QuoteCreateRequest request = QuoteCreateRequest.create("테스트용 비공개 문장입니다.", null);

    ResponseEntity<ApiResponse<QuoteResponse>> response =
        restTemplate.exchange(
            "/quotes/private",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            new ParameterizedTypeReference<>() {});

    assert response.getBody() != null;
    return response.getBody().data().getQuoteId();
  }

  @Nested
  @DisplayName("GET /admin/quotes - 문장 목록 조회")
  class GetQuotes {
    @Test
    @DisplayName("성공")
    void success() {
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuotePaginationResponse>> response =
          restTemplate.exchange(
              "/admin/quotes",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().success()).isTrue();
      assertThat(response.getBody().data().getContent()).isNotNull();
    }

    @Test
    @DisplayName("성공 - 페이지네이션")
    void successWithPagination() {
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuotePaginationResponse>> response =
          restTemplate.exchange(
              "/admin/quotes?page=1&size=3",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getPage()).isEqualTo(1);
      assertThat(response.getBody().data().getSize()).isEqualTo(3);
      assertThat(response.getBody().data().getContent().size()).isLessThanOrEqualTo(3);
      assertThat(response.getBody().data().isHasNext()).isTrue();
    }

    @Test
    @DisplayName("성공 - status 필터링")
    void successWithStatusFilter() {
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuotePaginationResponse>> response =
          restTemplate.exchange(
              "/admin/quotes?status=PENDING",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      List<QuoteResponse> content = response.getBody().data().getContent();

      assertThat(content).allMatch(q -> q.getStatus().name().equals(QuoteStatus.PENDING.name()));
    }

    @Test
    @DisplayName("성공 - type 필터링")
    void successWithTypeFilter() {
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuotePaginationResponse>> response =
          restTemplate.exchange(
              "/admin/quotes?type=PRIVATE",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      List<QuoteResponse> content = response.getBody().data().getContent();

      assertThat(content).allMatch(q -> q.getType().name().equals(QuoteType.PRIVATE.name()));
    }

    @Test
    @DisplayName("validation 실패 시 400")
    void badRequestWhenInvalid() {
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // page 0 이하
      assertThat(
              restTemplate
                  .exchange(
                      "/admin/quotes?page=0",
                      HttpMethod.GET,
                      new HttpEntity<>(headers),
                      new ParameterizedTypeReference<ApiResponse<QuotePaginationResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);

      // size 101 이상
      assertThat(
              restTemplate
                  .exchange(
                      "/admin/quotes?size=101",
                      HttpMethod.GET,
                      new HttpEntity<>(headers),
                      new ParameterizedTypeReference<ApiResponse<QuotePaginationResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuotePaginationResponse>> response =
          restTemplate.exchange(
              "/admin/quotes",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<QuotePaginationResponse>> response =
          restTemplate.exchange(
              "/admin/quotes", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /admin/quotes/{quoteId}/approve - 문장 승인")
  class ApproveQuote {
    @Test
    @DisplayName("성공")
    void success() {
      // PENDING 상태의 공개 문장 생성
      String userToken = getAccessToken(USER_PROVIDER_ID);
      Long quoteId = createPublicQuote(userToken);

      // 승인
      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/" + quoteId + "/approve",
              HttpMethod.POST,
              new HttpEntity<>(adminHeaders),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getStatus().name()).isEqualTo(QuoteStatus.ACTIVE.name());
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/4/approve",
              HttpMethod.POST,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/4/approve",
              HttpMethod.POST,
              null,
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /admin/quotes/{quoteId}/reject - 문장 거부")
  class RejectQuote {

    @Test
    @DisplayName("성공")
    void success() {
      // PENDING 상태의 공개 문장 생성
      String userToken = getAccessToken(USER_PROVIDER_ID);
      Long quoteId = createPublicQuote(userToken);

      // 거부
      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/" + quoteId + "/reject",
              HttpMethod.POST,
              new HttpEntity<>(adminHeaders),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getType().name()).isEqualTo("PRIVATE");
      assertThat(response.getBody().data().getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/4/reject",
              HttpMethod.POST,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/4/reject",
              HttpMethod.POST,
              null,
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("PATCH /admin/quotes/{quoteId} - 문장 수정")
  class UpdateQuote {

    @Test
    @DisplayName("성공")
    void success() {
      // 문장 생성 후 승인
      String userToken = getAccessToken(USER_PROVIDER_ID);
      Long quoteId = createPublicQuote(userToken);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      // 승인
      restTemplate.exchange(
          "/admin/quotes/" + quoteId + "/approve",
          HttpMethod.POST,
          new HttpEntity<>(adminHeaders),
          new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {});

      // 수정
      QuoteUpdateRequest updateRequest = QuoteUpdateRequest.create("관리자가 수정한 문장입니다.", "관리자");
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/" + quoteId,
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, adminHeaders),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getSentence()).isEqualTo("관리자가 수정한 문장입니다.");
      assertThat(response.getBody().data().getAuthor()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);
      QuoteUpdateRequest request = QuoteUpdateRequest.create("수정 시도", null);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/33",
              HttpMethod.PATCH,
              new HttpEntity<>(request, headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      QuoteUpdateRequest request = QuoteUpdateRequest.create("수정 시도", null);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/33",
              HttpMethod.PATCH,
              new HttpEntity<>(request),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("DELETE /admin/quotes/{quoteId} - 문장 삭제")
  class DeleteQuote {

    @Test
    @DisplayName("성공")
    void success() {
      // 문장 생성
      String userToken = getAccessToken(USER_PROVIDER_ID);
      Long quoteId = createPrivateQuote(userToken);

      // 삭제
      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/quotes/" + quoteId,
              HttpMethod.DELETE,
              new HttpEntity<>(adminHeaders),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      // 삭제 확인
      ResponseEntity<ApiResponse<QuoteResponse>> getResponse =
          restTemplate.exchange(
              "/quotes/" + quoteId,
              HttpMethod.GET,
              new HttpEntity<>(createAuthHeader(userToken)),
              new ParameterizedTypeReference<>() {});

      assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/quotes/33",
              HttpMethod.DELETE,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/quotes/33", HttpMethod.DELETE, null, new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("PATCH /admin/quotes/{quoteId}/hide - 문장 숨김")
  class HideQuote {

    @Test
    @DisplayName("성공")
    void success() {
      // 문장 생성 후 승인
      String userToken = getAccessToken(USER_PROVIDER_ID);
      Long quoteId = createPublicQuote(userToken);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      // 승인
      restTemplate.exchange(
          "/admin/quotes/" + quoteId + "/approve",
          HttpMethod.POST,
          new HttpEntity<>(adminHeaders),
          new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {});

      // 숨김
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/" + quoteId + "/hide",
              HttpMethod.PATCH,
              new HttpEntity<>(adminHeaders),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getStatus().name()).isEqualTo("HIDDEN");
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/33/hide",
              HttpMethod.PATCH,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/33/hide",
              HttpMethod.PATCH,
              null,
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /admin/quotes/{quoteId}/restore - 숨김 해제")
  class RestoreQuote {

    @Test
    @DisplayName("성공")
    void success() {
      // 문장 생성 → 승인 → 숨김
      String userToken = getAccessToken(USER_PROVIDER_ID);
      Long quoteId = createPublicQuote(userToken);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      // 승인
      restTemplate.exchange(
          "/admin/quotes/" + quoteId + "/approve",
          HttpMethod.POST,
          new HttpEntity<>(adminHeaders),
          new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {});

      // 숨김
      restTemplate.exchange(
          "/admin/quotes/" + quoteId + "/hide",
          HttpMethod.PATCH,
          new HttpEntity<>(adminHeaders),
          new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {});

      // 숨김 해제
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/" + quoteId + "/restore",
              HttpMethod.POST,
              new HttpEntity<>(adminHeaders),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/33/restore",
              HttpMethod.POST,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<QuoteResponse>> response =
          restTemplate.exchange(
              "/admin/quotes/33/restore",
              HttpMethod.POST,
              null,
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }
}
