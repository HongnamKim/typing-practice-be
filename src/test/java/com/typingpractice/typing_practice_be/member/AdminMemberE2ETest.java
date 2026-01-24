package com.typingpractice.typing_practice_be.member;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.auth.dto.TestLoginRequest;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.dto.MemberResponse;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberBanRequest;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberUpdateRoleRequest;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AdminMemberE2ETest {
  @Autowired private TestRestTemplate restTemplate;

  @Autowired private EntityManager em;

  private static final String ADMIN_PROVIDER_ID = "0";
  private static final String USER_PROVIDER_ID = "1";

  private String getAccessToken(String providerId) {
    TestLoginRequest request = TestLoginRequest.create(providerId);
    ResponseEntity<Map> response = restTemplate.postForEntity("/auth/test", request, Map.class);

    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return (String) data.get("accessToken");
  }

  private HttpHeaders createAuthHeader(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  @Nested
  @DisplayName("GET /admin/members - 회원 목록 조회")
  class GetMembers {
    @Test
    @DisplayName("성공 - 기본 파라미터")
    void success() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");

      assertThat(data.get("page")).isEqualTo(1);
      assertThat(data.get("size")).isEqualTo(50);
      assertThat(data.get("content")).isNotNull();
    }

    @Test
    @DisplayName("성공 - 페이지네이션")
    void successWithPagination() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members?page=1&size=3", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      assertThat(data.get("page")).isEqualTo(1);
      assertThat(data.get("size")).isEqualTo(3);
      List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
      assertThat(content.size()).isLessThanOrEqualTo(3);
      assertThat(data.get("hasNext")).isEqualTo(true);
    }

    @Test
    @DisplayName("성공 - role 필터링")
    void successWithRoleFilter() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members?role=USER", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
      assertThat(content).allMatch(member -> member.get("role").equals("USER"));
    }

    @Test
    @DisplayName("성공 - 정렬")
    void successWithSort() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members?orderBy=nickname&sortDirection=ASC&size=5",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");

      for (int i = 0; i < content.size() - 1; i++) {
        String current = (String) content.get(i).get("nickname");
        String next = (String) content.get(i + 1).get("nickname");
        assertThat(current.compareTo(next)).isLessThanOrEqualTo(0);
      }
    }

    @Test
    @DisplayName("DTO validation 실패 시 400")
    void badRequestWhenInvalid() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // when
      // page 0 이하
      ResponseEntity<Map> invalidPage =
          restTemplate.exchange(
              "/admin/members?page=0", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
      ResponseEntity<Map> invalidSize =
          restTemplate.exchange(
              "/admin/members?size=1001", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
      ResponseEntity<Map> invalidSortDirection =
          restTemplate.exchange(
              "/admin/members?sortDirection=down",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              Map.class);
      ResponseEntity<Map> invalidRole =
          restTemplate.exchange(
              "/admin/members?role=MEMBER", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
      ResponseEntity<Map> invalidOrderBy =
          restTemplate.exchange(
              "/admin/members?orderBy=role", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(invalidPage.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(invalidSize.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(invalidSortDirection.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(invalidRole.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(invalidOrderBy.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      // given
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      // when
      ResponseEntity<Map> response = restTemplate.getForEntity("/admin/members", Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /admin/members/{memberId} - 회원 상세 조회")
  class GetMemberById {
    @Test
    @DisplayName("성공")
    void success() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/2", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      assertThat(data.get("id")).isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 404")
    void notFound() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/9999", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      // given
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/2", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      // when
      ResponseEntity<Map> response = restTemplate.getForEntity("/admin/members/2", Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("PATCH /admin/members/{memberId}/role - 회원 역할 변경")
  class UpdateMemberRole {
    @Test
    @DisplayName("역할 변경 성공")
    void success() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);
      MemberUpdateRoleRequest request = MemberUpdateRoleRequest.create(MemberRole.BANNED);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/2/role",
              HttpMethod.PATCH,
              new HttpEntity<>(request, headers),
              Map.class);

      ResponseEntity<Map> memberResponse =
          restTemplate.exchange(
              "/admin/members/2", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      System.out.println("memberResponse = " + memberResponse);
      assertThat(((Map<String, Object>) memberResponse.getBody().get("data")).get("role"))
          .isEqualTo("BANNED");
    }

    @Test
    @DisplayName("ADMIN 역할 변경 시도 - 400")
    void badRequestWhenTargetIsAdmin() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);
      MemberUpdateRoleRequest request = MemberUpdateRoleRequest.create(MemberRole.USER);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/1/role",
              HttpMethod.PATCH,
              new HttpEntity<>(request, headers),
              Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      // given
      String token = getAccessToken(USER_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);
      MemberUpdateRoleRequest request = MemberUpdateRoleRequest.create(MemberRole.BANNED);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/2/role",
              HttpMethod.PATCH,
              new HttpEntity<>(request, headers),
              Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      // given
      MemberUpdateRoleRequest request = MemberUpdateRoleRequest.create(MemberRole.BANNED);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/2/role", HttpMethod.PATCH, new HttpEntity<>(request), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /admin/members/{memberId]/ban - 회원 밴")
  class BanMember {
    @Test
    @DisplayName("성공")
    void success() {
      // given
      String token = getAccessToken(ADMIN_PROVIDER_ID);
      HttpHeaders headers = createAuthHeader(token);
      MemberBanRequest request = MemberBanRequest.create("테스트 밴");

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/2/ban",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      assertThat(data.get("role")).isEqualTo("BANNED");
    }

    @Test
    @DisplayName("성공 - banReason 없이")
    void successWithoutReason() {
      // given
      String token = getAccessToken("0");
      HttpHeaders headers = createAuthHeader(token);
      MemberBanRequest request = MemberBanRequest.create(null);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/3/ban",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      assertThat(data.get("role")).isEqualTo("BANNED");
    }

    @Test
    @DisplayName("ADMIN 밴 시도 - 400")
    void badRequestWhenTargetIsAdmin() {
      // given
      String token = getAccessToken("0");
      HttpHeaders headers = createAuthHeader(token);
      MemberBanRequest request = MemberBanRequest.create("밴 테스트");

      // when - admin (member_id=1)
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/1/ban",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      // given
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);
      MemberBanRequest request = MemberBanRequest.create("밴 테스트");

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/2/ban",
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      // given
      MemberBanRequest request = MemberBanRequest.create("밴 테스트");

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/2/ban", HttpMethod.POST, new HttpEntity<>(request), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /admin/members/{memberId}/unban - 회원 밴 해제")
  class UnbanMember {

    @Test
    @DisplayName("성공")
    void success() {
      // given
      String token = getAccessToken("0");
      HttpHeaders headers = createAuthHeader(token);

      // 먼저 밴 처리
      MemberBanRequest banRequest = MemberBanRequest.create("밴 테스트");
      restTemplate.exchange(
          "/admin/members/4/ban",
          HttpMethod.POST,
          new HttpEntity<>(banRequest, headers),
          Map.class);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/4/unban", HttpMethod.POST, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      assertThat(data.get("role")).isEqualTo("USER");
    }

    @Test
    @DisplayName("관리자 아닌 유저 - 403")
    void forbiddenWhenNotAdmin() {
      // given
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members/2/unban", HttpMethod.POST, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      // when
      ResponseEntity<Map> response =
          restTemplate.exchange("/admin/members/2/unban", HttpMethod.POST, null, Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }
}
