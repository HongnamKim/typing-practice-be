package com.typingpractice.typing_practice_be.report;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.BaseE2ETest;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuoteResponse;
import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

public class AdminReportE2ETest extends BaseE2ETest {
  private static final ParameterizedTypeReference<ApiResponse<QuoteResponse>> QUOTE_RESPONSE =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<ReportResponse>> REPORT_RESPONSE =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<AdminReportPaginationResponse>>
      ADMIN_REPORT_PAGE_RESPONSE = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<ApiResponse<Void>> VOID_RESPONSE =
      new ParameterizedTypeReference<>() {};

  private Long createPublicQuoteAndApprove() {
    String userToken = getAccessToken(USER_PROVIDER_ID);
    HttpHeaders userHeaders = createAuthHeader(userToken);

    QuoteCreateRequest request = QuoteCreateRequest.create("신고 테스트용 문장", null);
    ResponseEntity<ApiResponse<QuoteResponse>> createResponse =
        restTemplate.exchange(
            "/quotes/public",
            HttpMethod.POST,
            new HttpEntity<>(request, userHeaders),
            QUOTE_RESPONSE);

    assert createResponse.getBody() != null;
    Long quoteId = createResponse.getBody().data().getQuoteId();

    String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
    HttpHeaders adminHeaders = createAuthHeader(adminToken);
    restTemplate.exchange(
        "/admin/quotes/" + quoteId + "/approve",
        HttpMethod.POST,
        new HttpEntity<>(adminHeaders),
        QUOTE_RESPONSE);

    return quoteId;
  }

  private Long createReport(Long quoteId) {
    String token = getAccessToken(USER_PROVIDER_ID);
    HttpHeaders headers = createAuthHeader(token);

    ReportCreateRequest request =
        ReportCreateRequest.create(quoteId, ReportReason.MODIFY, "오타 수정 필요");
    ResponseEntity<ApiResponse<ReportResponse>> response =
        restTemplate.exchange(
            "/reports",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            new ParameterizedTypeReference<>() {});

    assert response.getBody() != null;
    return response.getBody().data().getId();
  }

  @Nested
  @DisplayName("GET /admin/reports - 신고 목록 조회")
  class GetReports {
    @Test
    @DisplayName("성공")
    void success() {
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<AdminReportPaginationResponse>> response =
          restTemplate.exchange(
              "/admin/reports",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ADMIN_REPORT_PAGE_RESPONSE);

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

      ResponseEntity<ApiResponse<AdminReportPaginationResponse>> response =
          restTemplate.exchange(
              "/admin/reports?page=1&size=3",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ADMIN_REPORT_PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getPage()).isEqualTo(1);
      assertThat(response.getBody().data().getSize()).isEqualTo(3);
      assertThat(response.getBody().data().getContent().size()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("성공 - status 필터링")
    void successWithStatusFilter() {
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<AdminReportPaginationResponse>> response =
          restTemplate.exchange(
              "/admin/reports?status=PENDING",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ADMIN_REPORT_PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      List<AdminReportResponse> content = response.getBody().data().getContent();
      assertThat(content).allMatch(r -> r.getStatus() == ReportStatus.PENDING);
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
                      "/admin/reports?page=-1",
                      HttpMethod.GET,
                      new HttpEntity<>(headers),
                      ADMIN_REPORT_PAGE_RESPONSE)
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);

      // size 101 이상
      assertThat(
              restTemplate
                  .exchange(
                      "/admin/reports?size=101",
                      HttpMethod.GET,
                      new HttpEntity<>(headers),
                      ADMIN_REPORT_PAGE_RESPONSE)
                  .getStatusCode())
          .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<AdminReportPaginationResponse>> response =
          restTemplate.exchange(
              "/admin/reports",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ADMIN_REPORT_PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<AdminReportPaginationResponse>> response =
          restTemplate.exchange("/admin/reports", HttpMethod.GET, null, ADMIN_REPORT_PAGE_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /admin/reports/{reportId} - 신고 상세 조회")
  class GetReportById {
    @Test
    @DisplayName("성공")
    void success() {
      Long quoteId = createPublicQuoteAndApprove();
      Long reportId = createReport(quoteId);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<ReportResponse>> response =
          restTemplate.exchange(
              "/admin/reports/" + reportId,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              REPORT_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assert response.getBody() != null;
      assertThat(response.getBody().data().getId()).isEqualTo(reportId);
      assertThat(response.getBody().data().getReason()).isEqualTo(ReportReason.MODIFY);
      assertThat(response.getBody().data().getStatus()).isEqualTo(ReportStatus.PENDING);
      assertThat(response.getBody().data().getDetail()).isEqualTo("오타 수정 필요");
    }

    @Test
    @DisplayName("존재하지 않는 신고 - 404")
    void notFound() {
      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<ReportResponse>> response =
          restTemplate.exchange(
              "/admin/reports/999999",
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              REPORT_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<ReportResponse>> response =
          restTemplate.exchange(
              "/admin/reports/1", HttpMethod.GET, new HttpEntity<>(headers), REPORT_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<ReportResponse>> response =
          restTemplate.exchange("/admin/reports/1", HttpMethod.GET, null, REPORT_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /admin/reports/{quoteId}/process - 신고 처리")
  class ProcessReport {
    @Test
    @DisplayName("성공 - 문장 수정")
    void successWithUpdate() {
      Long quoteId = createPublicQuoteAndApprove();
      Long reportId = createReport(quoteId);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ReportProcessRequest request = ReportProcessRequest.create("수정된 문장입니다.", "수정된 저자");

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/reports/" + quoteId + "/process",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      // 문장 수정 확인
      String userToken = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders userHeaders = createAuthHeader(userToken);
      ResponseEntity<ApiResponse<QuoteResponse>> quoteResponse =
          restTemplate.exchange(
              "/quotes/" + quoteId, HttpMethod.GET, new HttpEntity<>(userHeaders), QUOTE_RESPONSE);

      assert quoteResponse.getBody() != null;
      assertThat(quoteResponse.getBody().data().getSentence()).isEqualTo("수정된 문장입니다.");
      assertThat(quoteResponse.getBody().data().getAuthor()).isEqualTo("수정된 저자");

      // 신고 상태 PROCESSED 확인
      ResponseEntity<ApiResponse<ReportResponse>> reportResponse =
          restTemplate.exchange(
              "/admin/reports/" + reportId,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              REPORT_RESPONSE);

      assert reportResponse.getBody() != null;
      assertThat(reportResponse.getBody().data().getStatus()).isEqualTo(ReportStatus.PROCESSED);
      assertThat(reportResponse.getBody().data().isQuoteDeleted()).isFalse();
    }

    @Test
    @DisplayName("성공 - 문장 삭제")
    void successWithDelete() {
      Long quoteId = createPublicQuoteAndApprove();
      Long reportId = createReport(quoteId);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ReportProcessRequest request = ReportProcessRequest.create(null, null);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/reports/" + quoteId + "/process",
              HttpMethod.POST,
              new HttpEntity<>(request, adminHeaders),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      // 문장 삭제 확인
      String userToken = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders userHeaders = createAuthHeader(userToken);

      ResponseEntity<ApiResponse<QuoteResponse>> quoteResponse =
          restTemplate.exchange(
              "/quotes/" + quoteId, HttpMethod.GET, new HttpEntity<>(userHeaders), QUOTE_RESPONSE);

      assertThat(quoteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

      // 신고 상태 PROCESSED, quoteDeleted=true 확인
      ResponseEntity<ApiResponse<ReportResponse>> reportResponse =
          restTemplate.exchange(
              "/admin/reports/" + reportId,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              REPORT_RESPONSE);

      assert reportResponse.getBody() != null;
      assertThat(reportResponse.getBody().data().getStatus()).isEqualTo(ReportStatus.PROCESSED);
      assertThat(reportResponse.getBody().data().isQuoteDeleted()).isTrue();
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ReportProcessRequest request = ReportProcessRequest.create("수정 시도", null);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/reports/1/process",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ReportProcessRequest request = ReportProcessRequest.create("수정 시도", null);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/reports/1/process",
              HttpMethod.POST,
              new HttpEntity<>(request),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("DELETE /admin/reports/{reportId} - 신고 삭제")
  class DeleteReport {
    @Test
    @DisplayName("성공")
    void success() {
      Long quoteId = createPublicQuoteAndApprove();
      Long reportId = createReport(quoteId);

      String adminToken = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders adminHeaders = createAuthHeader(adminToken);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/reports/" + reportId,
              HttpMethod.DELETE,
              new HttpEntity<>(adminHeaders),
              VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      // 삭제 확인 - 조회 시 404
      ResponseEntity<ApiResponse<ReportResponse>> getResponse =
          restTemplate.exchange(
              "/admin/reports/" + reportId,
              HttpMethod.GET,
              new HttpEntity<>(adminHeaders),
              REPORT_RESPONSE);

      assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange(
              "/admin/reports/1", HttpMethod.DELETE, new HttpEntity<>(headers), VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      ResponseEntity<ApiResponse<Void>> response =
          restTemplate.exchange("/admin/reports/1", HttpMethod.DELETE, null, VOID_RESPONSE);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }
}
