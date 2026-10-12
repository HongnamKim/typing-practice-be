package com.typingpractice.typing_practice_be.member;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.auth.dto.TestLoginRequest;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberBanRequest;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberUpdateRoleRequest;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import com.typingpractice.typing_practice_be.typingrecord.statistics.repository.MemberTypingStatsRepository;
import com.typingpractice.typing_practice_be.word.domain.WordLanguage;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.domain.MemberWordTypingStats;
import com.typingpractice.typing_practice_be.wordtypingrecord.statistics.repository.MemberWordTypingStatsRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AdminMemberE2ETest {
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private MemberRepository memberRepository;
  @Autowired private MemberTypingStatsRepository memberTypingStatsRepository;
  @Autowired private MemberWordTypingStatsRepository memberWordTypingStatsRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  private static final String ADMIN_PROVIDER_ID = "0";
  private static final String USER_PROVIDER_ID = "1";

  // 최근 타이핑 시각 시드: 유저1 = 문장 KOREAN 10-01 / ENGLISH 10-05, 유저2 = 단어 KOREAN 10-08, 유저3 = 없음
  private static final LocalDateTime USER1_LAST = LocalDateTime.of(2026, 10, 5, 10, 0);
  private static final LocalDateTime USER2_LAST = LocalDateTime.of(2026, 10, 8, 10, 0);

  // 레포지토리가 getResultStream 을 쓰므로 트랜잭션 안에서 조회
  private Member findByProviderId(String providerId) {
    return transactionTemplate.execute(
        status -> memberRepository.findByProviderId(providerId).orElseThrow());
  }

  private void seedLastPracticedAt() {
    transactionTemplate.executeWithoutResult(
        status -> {
          Member user1 = findByProviderId("1");
          Member user2 = findByProviderId("2");

          if (memberTypingStatsRepository
              .findByMemberIdAndLanguage(user1.getId(), QuoteLanguage.KOREAN)
              .isEmpty()) {
            memberTypingStatsRepository.save(
                MemberTypingStats.create(
                    user1, QuoteLanguage.KOREAN, 1, 300f, 0.9f, 300, 1f, 0,
                    LocalDateTime.of(2026, 10, 1, 10, 0)));
            memberTypingStatsRepository.save(
                MemberTypingStats.create(
                    user1, QuoteLanguage.ENGLISH, 1, 300f, 0.9f, 300, 1f, 0, USER1_LAST));
          }
          if (memberWordTypingStatsRepository
              .findByMemberIdAndLanguage(user2.getId(), WordLanguage.KOREAN)
              .isEmpty()) {
            memberWordTypingStatsRepository.save(
                MemberWordTypingStats.create(
                    user2, WordLanguage.KOREAN, 1, 10, 60f, 0.9f, 60f, 1f, USER2_LAST));
          }
        });
  }

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
    @DisplayName("성공 - 최근 타이핑 시각(lastPracticedAt) 포함")
    void successIncludesLastPracticedAt() {
      // given
      seedLastPracticedAt();
      Long user1Id = findByProviderId("1").getId();
      Long user2Id = findByProviderId("2").getId();
      Long user3Id = findByProviderId("3").getId();
      HttpHeaders headers = createAuthHeader(getAccessToken(ADMIN_PROVIDER_ID));

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/admin/members?size=100", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");

      Map<Long, Map<String, Object>> byId = new java.util.HashMap<>();
      content.forEach(m -> byId.put(((Number) m.get("id")).longValue(), m));

      assertThat(byId.get(user1Id)).containsKey("lastPracticedAt");
      assertThat(LocalDateTime.parse((String) byId.get(user1Id).get("lastPracticedAt")))
          .isEqualTo(USER1_LAST);
      assertThat(LocalDateTime.parse((String) byId.get(user2Id).get("lastPracticedAt")))
          .isEqualTo(USER2_LAST);
      assertThat(byId.get(user3Id)).containsKey("lastPracticedAt");
      assertThat(byId.get(user3Id).get("lastPracticedAt")).isNull();
    }

    @Test
    @DisplayName("성공 - 최근 타이핑 시각 정렬 (null 은 마지막)")
    void successWithSortByLastPracticedAt() {
      // given
      seedLastPracticedAt();
      Long user1Id = findByProviderId("1").getId();
      Long user2Id = findByProviderId("2").getId();
      HttpHeaders headers = createAuthHeader(getAccessToken(ADMIN_PROVIDER_ID));

      // when
      ResponseEntity<Map> desc =
          restTemplate.exchange(
              "/admin/members?orderBy=lastPracticedAt&sortDirection=DESC&size=100",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              Map.class);
      ResponseEntity<Map> asc =
          restTemplate.exchange(
              "/admin/members?orderBy=lastPracticedAt&sortDirection=ASC&size=100",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              Map.class);

      // then
      assertThat(desc.getStatusCode()).isEqualTo(HttpStatus.OK);
      List<Map<String, Object>> descContent =
          (List<Map<String, Object>>) ((Map<String, Object>) desc.getBody().get("data")).get("content");
      List<Long> descIds =
          descContent.stream().map(m -> ((Number) m.get("id")).longValue()).toList();
      assertThat(descIds.get(0)).isEqualTo(user2Id);
      assertThat(descIds.get(1)).isEqualTo(user1Id);
      assertThat(descContent.subList(2, descContent.size()))
          .allMatch(m -> m.get("lastPracticedAt") == null);

      assertThat(asc.getStatusCode()).isEqualTo(HttpStatus.OK);
      List<Map<String, Object>> ascContent =
          (List<Map<String, Object>>) ((Map<String, Object>) asc.getBody().get("data")).get("content");
      List<Long> ascIds =
          ascContent.stream().map(m -> ((Number) m.get("id")).longValue()).toList();
      assertThat(ascIds.get(0)).isEqualTo(user1Id);
      assertThat(ascIds.get(1)).isEqualTo(user2Id);
      assertThat(ascContent.subList(2, ascContent.size()))
          .allMatch(m -> m.get("lastPracticedAt") == null);
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
