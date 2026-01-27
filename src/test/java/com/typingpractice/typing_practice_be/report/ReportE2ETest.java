package com.typingpractice.typing_practice_be.report;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.BaseE2ETest;
import com.typingpractice.typing_practice_be.auth.dto.LoginResponse;
import com.typingpractice.typing_practice_be.auth.dto.TestLoginRequest;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberBanRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.dto.ReportCreateRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportPaginationResponse;
import com.typingpractice.typing_practice_be.report.dto.ReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReportE2ETest extends BaseE2ETest {

  private Long createPublicQuoteAndApprove() {
    String userToken = getAccessToken(USER_PROVIDER_ID);
    HttpHeaders userHeaders = createAuthHeader(userToken);

    QuoteCreateRequest request = QuoteCreateRequest.create("신고 테스트용 문장입니다.", null);
    ResponseEntity<ApiResponse<QuoteResponse>> createResponse =
        restTemplate.exchange(
            "/quotes/public",
            HttpMethod.POST,
            new HttpEntity<>(request, userHeaders),
            new ParameterizedTypeReference<>() {});

    assert createResponse.getBody() != null;
    Long quoteId = createResponse.getBody().data().getQuoteId();

    // 승인
    String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
    HttpHeaders adminHeaders = createAuthHeader(adminToken);
    restTemplate.exchange(
        "/admin/quotes/" + quoteId + "/approve",
        HttpMethod.POST,
        new HttpEntity<>(adminHeaders),
        new ParameterizedTypeReference<ApiResponse<QuoteResponse>>() {});

    return quoteId;
  }

  @Nested
  @DisplayName("POST /reports - 신고 생성")
  class CreateReport {
    @Test
    @DisplayName("성공")
    void success() {
      Long quoteId = createPublicQuoteAndApprove();

      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);
      ReportCreateRequest request = ReportCreateRequest.create(quoteId, ReportReason.MODIFY, "오타");

      ResponseEntity<ApiResponse<ReportResponse>> response =
          restTemplate.exchange(
              "/reports",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getReason()).isEqualTo(ReportReason.MODIFY);
      assertThat(response.getBody().data().getStatus().name())
          .isEqualTo(ReportStatus.PENDING.name());
    }

    @Test
    @DisplayName("validation 실패 시 400")
    void badRequestWhenInvalid() {
      Long quoteId = createPublicQuoteAndApprove();

      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // quoteId null
      assertThat(
              restTemplate
                  .exchange(
                      "/reports",
                      HttpMethod.POST,
                      new HttpEntity<>(
                          ReportCreateRequest.create(null, ReportReason.MODIFY, null), headers),
                      new ParameterizedTypeReference<ApiResponse<ReportResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);

      // reason null
      assertThat(
              restTemplate
                  .exchange(
                      "/reports",
                      HttpMethod.POST,
                      new HttpEntity<>(ReportCreateRequest.create(quoteId, null, null), headers),
                      new ParameterizedTypeReference<ApiResponse<ReportResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);

      // detail 200자 초과
      assertThat(
              restTemplate
                  .exchange(
                      "/reports",
                      HttpMethod.POST,
                      new HttpEntity<>(
                          ReportCreateRequest.create(quoteId, ReportReason.MODIFY, "a".repeat(201)),
                          headers),
                      new ParameterizedTypeReference<ApiResponse<ReportResponse>>() {})
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("BANNED 유저 - 403")
    void forbiddenWhenBanned() {
      Long quoteId = createPublicQuoteAndApprove();

      // 새 유저 생성 후 ban 처리
      String uniqueProviderId = "banned-report-" + System.currentTimeMillis();

      // admin 으로 ban 처리
      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<LoginResponse>> loginResponse =
          restTemplate.exchange(
              "/auth/test",
              HttpMethod.POST,
              new HttpEntity<>(TestLoginRequest.create(uniqueProviderId)),
              new ParameterizedTypeReference<>() {});
      assert loginResponse.getBody() != null;
      Long memberId = loginResponse.getBody().data().getId();

      restTemplate.exchange(
          "/admin/members/" + memberId + "/ban",
          HttpMethod.POST,
          new HttpEntity<>(MemberBanRequest.create(null), adminHeaders),
          new ParameterizedTypeReference<>() {});

      // BANNED 토큰 발급
      String bannedToken = getAccessToken(uniqueProviderId);
      HttpHeaders bannedHeaders = createAuthHeader(bannedToken);

      ReportCreateRequest request = ReportCreateRequest.create(quoteId, ReportReason.MODIFY, null);

      ResponseEntity<ApiResponse<ReportResponse>> response =
          restTemplate.exchange(
              "/reports",
              HttpMethod.POST,
              new HttpEntity<>(request, bannedHeaders),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ReportCreateRequest request = ReportCreateRequest.create(1L, ReportReason.MODIFY, null);

      ResponseEntity<ApiResponse<ReportResponse>> response =
          restTemplate.exchange(
              "/reports",
              HttpMethod.POST,
              new HttpEntity<>(request),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /reports/my - 내 신고 목록 조회")
  class GetMyReports {
    @Test
    @DisplayName("성공")
    void success() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<ReportPaginationResponse>> response =
          restTemplate.exchange(
              "/reports/my",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().success()).isTrue();
      assertThat(response.getBody().data().getContent()).isNotNull();
    }

    @Test
    @DisplayName("생성한 신고 조회")
    void containsMyReport() {
      Long quoteId = createPublicQuoteAndApprove();

      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // 신고 생성
      ReportCreateRequest createRequest =
          ReportCreateRequest.create(quoteId, ReportReason.MODIFY, null);
      ResponseEntity<ApiResponse<ReportResponse>> createResponse =
          restTemplate.exchange(
              "/reports",
              HttpMethod.POST,
              new HttpEntity<>(createRequest, headers),
              new ParameterizedTypeReference<>() {});
      assert createResponse.getBody() != null;
      Long reportId = createResponse.getBody().data().getId();

      // 신고 조회
      ResponseEntity<ApiResponse<ReportPaginationResponse>> response =
          restTemplate.exchange(
              "/reports/my",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      List<Long> reportIds =
          response.getBody().data().getContent().stream().map(ReportResponse::getId).toList();
      assertThat(reportIds).contains(reportId);
    }

    @Test
    @DisplayName("페이지네이션 동작 확인")
    void pagination() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<ReportPaginationResponse>> response =
          restTemplate.exchange(
              "/reports/my?page=1&size=3",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getPage()).isEqualTo(1);
      assertThat(response.getBody().data().getSize()).isEqualTo(3);
      assertThat(response.getBody().data().getContent().size()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<ReportPaginationResponse>> response =
          restTemplate.exchange(
              "/reports/my", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("DELETE /reports/{reportId} - 내 신고 삭제")
  class DeleteReport {

    @Test
    @DisplayName("성공")
    void success() {
      Long quoteId = createPublicQuoteAndApprove();

      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // 신고 생성
      ReportCreateRequest createRequest =
          ReportCreateRequest.create(quoteId, ReportReason.MODIFY, "삭제 테스트");
      ResponseEntity<ApiResponse<ReportResponse>> createResponse =
          restTemplate.exchange(
              "/reports",
              HttpMethod.POST,
              new HttpEntity<>(createRequest, headers),
              new ParameterizedTypeReference<>() {});
      assert createResponse.getBody() != null;
      Long reportId = createResponse.getBody().data().getId();

      // 삭제
      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/reports/" + reportId,
              HttpMethod.DELETE,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      // 삭제 확인 - 내 신고 목록에서 조회되지 않음
      ResponseEntity<ApiResponse<ReportPaginationResponse>> listResponse =
          restTemplate.exchange(
              "/reports/my",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              new ParameterizedTypeReference<>() {});

      assert listResponse.getBody() != null;
      List<Long> reportIds =
          listResponse.getBody().data().getContent().stream().map(ReportResponse::getId).toList();
      assertThat(reportIds).doesNotContain(reportId);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/reports/1", HttpMethod.DELETE, null, new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }
}
