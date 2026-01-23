package com.typingpractice.typing_practice_be.member;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.auth.dto.AuthTokenRefreshRequest;
import com.typingpractice.typing_practice_be.auth.dto.TestLoginRequest;
import com.typingpractice.typing_practice_be.member.dto.UpdateNicknameRequest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MemberE2ETest {
  @Autowired private TestRestTemplate restTemplate;

  private Map<String, Object> login(String providerId) {
    TestLoginRequest request = TestLoginRequest.create(providerId);
    ResponseEntity<Map> response = restTemplate.postForEntity("/auth/test", request, Map.class);
    return (Map<String, Object>) response.getBody().get("data");
  }

  private String getAccessToken(String providerId) {
    return (String) login(providerId).get("accessToken");
  }

  private String getRefreshToken(String providerId) {
    return (String) login(providerId).get("refreshToken");
  }

  private HttpHeaders createAuthHeader(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  @Nested
  @DisplayName("GET /members/me - 내 정보 조회")
  class GetMe {
    @Test
    @DisplayName("성공")
    void success() {
      // given
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/members/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      assertThat(data.get("id")).isNotNull();
      assertThat(data.get("email")).isEqualTo("user1@test.com");
      assertThat(data.get("nickname")).isEqualTo("유저1");
      assertThat(data.get("role")).isEqualTo("USER");
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      // when
      ResponseEntity<Map> response = restTemplate.getForEntity("/members/me", Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("GET /members/check-nickname - 닉네임 중복 확인")
  class CheckNickname {
    @Test
    @DisplayName("중복된 닉네임이면 true")
    void duplicatedNickname() {
      // given
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      // when - 기존 유저의 닉네임 사용
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/members/check-nickname?nickname=유저1",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().get("data")).isEqualTo(true);
    }

    @Test
    @DisplayName("중복되지 않은 닉네임이면 false")
    void notDuplicatedNickname() {
      // given
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);
      // String uniqueNickname = "unique" + System.currentTimeMillis();

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/members/check-nickname?nickname=unique",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().get("data")).isEqualTo(false);
    }

    @Test
    @DisplayName("닉네임 validation 실패 시 400")
    void badRequestWhenNicknameInvalid() {
      // given
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> shortNickname =
          restTemplate.exchange(
              "/members/check-nickname?nickname=a",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              Map.class);

      ResponseEntity<Map> longNickname =
          restTemplate.exchange(
              "/members/check-nickname?nickname=12345678901",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              Map.class);

      ResponseEntity<Map> blankNickname =
          restTemplate.exchange(
              "/members/check-nickname?nickname=     ",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              Map.class);

      // then
      // 2자 미만
      assertThat(shortNickname.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(longNickname.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(blankNickname.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      // when
      ResponseEntity<Map> response =
          restTemplate.getForEntity("/members/check-nickname?nickname=test", Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("PATCH /members/me - 닉네임 변경")
  class UpdateNickname {
    @Test
    @DisplayName("닉네임 변경 성공")
    void success() {
      // given
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);
      String newNickname = "새로운 닉네임";
      UpdateNicknameRequest request = UpdateNicknameRequest.create(newNickname);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/members/me", HttpMethod.PATCH, new HttpEntity<>(request, headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
      assertThat(data.get("nickname")).isEqualTo(newNickname);
    }

    @Test
    @DisplayName("중복된 닉네임으로 변경 시 에러")
    void duplicatedNickname() {
      // given
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);
      UpdateNicknameRequest request = UpdateNicknameRequest.create("유저2");

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/members/me", HttpMethod.PATCH, new HttpEntity<>(request, headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("닉네임 validation 실패 시 400")
    void badRequestWhenNicknameInvalid() {
      // given
      String token = getAccessToken("1");
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> shortNickname =
          restTemplate.exchange(
              "/members/me",
              HttpMethod.PATCH,
              new HttpEntity<>(UpdateNicknameRequest.create("a"), headers),
              Map.class);

      ResponseEntity<Map> longNickname =
          restTemplate.exchange(
              "/members/me",
              HttpMethod.PATCH,
              new HttpEntity<>(UpdateNicknameRequest.create("12345678901"), headers),
              Map.class);

      ResponseEntity<Map> blankNickname =
          restTemplate.exchange(
              "/members/me",
              HttpMethod.PATCH,
              new HttpEntity<>(UpdateNicknameRequest.create("   "), headers),
              Map.class);

      // then
      assertThat(shortNickname.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(longNickname.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(blankNickname.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      // given
      UpdateNicknameRequest request = UpdateNicknameRequest.create("newNickname");

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/members/me", HttpMethod.PATCH, new HttpEntity<>(request), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("DELETE /members/me - 회원 탈퇴")
  class DeleteMember {
    @Test
    @DisplayName("성공")
    void success() {
      // given
      String uniqueProviderId = "delete-test-" + System.currentTimeMillis();
      String token = getAccessToken(uniqueProviderId);
      HttpHeaders headers = createAuthHeader(token);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/members/me", HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("토큰 없이 요청 - 401")
    void unauthorizedWithoutToken() {
      // when
      ResponseEntity<Map> response =
          restTemplate.exchange("/members/me", HttpMethod.DELETE, null, Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("토큰 재발급 테스트")
  class TokenRotation {
    @Test
    @DisplayName("로그아웃 후 기존 토큰으로 요청 - 401")
    void accessDeniedAfterLogout() {
      // given
      Map<String, Object> loginData = login("1");
      String accessToken = (String) loginData.get("accessToken");
      HttpHeaders headers = createAuthHeader(accessToken);

      // 로그아웃
      restTemplate.exchange("/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Map.class);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/members/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("토큰 갱신 후 기존 리프레시 토큰 사용 불가")
    void oldRefreshTokenInvalidAfterRotation() {
      // given
      String refreshToken = getRefreshToken("1");
      AuthTokenRefreshRequest request = AuthTokenRefreshRequest.create(refreshToken);

      // 첫 번째 갱신 - 성공
      ResponseEntity<Map> firstResponse =
          restTemplate.postForEntity("/auth/refresh", request, Map.class);
      assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

      // when - 같은 리프레시 토큰으로 다시 갱신 시도
      ResponseEntity<Map> secondResponse =
          restTemplate.postForEntity("/auth/refresh", request, Map.class);

      // then
      assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("토큰 갱신 후 새 엑세스 토큰으로 API 호출 성공")
    void newAccessTokenWorksAfterRotation() {
      // given
      String refreshToken = getRefreshToken("1");
      AuthTokenRefreshRequest request = AuthTokenRefreshRequest.create(refreshToken);

      // 토큰 갱신
      ResponseEntity<Map> refreshResponse =
          restTemplate.postForEntity("/auth/refresh", request, Map.class);
      Map<String, Object> refreshData = (Map<String, Object>) refreshResponse.getBody().get("data");
      String newAccessToken = (String) refreshData.get("accessToken");

      // when - 새 토큰으로 API 호출
      HttpHeaders headers = createAuthHeader(newAccessToken);
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/members/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
  }
}
