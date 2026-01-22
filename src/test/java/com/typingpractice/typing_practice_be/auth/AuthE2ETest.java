package com.typingpractice.typing_practice_be.auth;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.auth.dto.AuthTokenRefreshRequest;
import com.typingpractice.typing_practice_be.auth.dto.TestLoginRequest;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleLoginRequest;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleTokenResponse;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleUserInfo;
import com.typingpractice.typing_practice_be.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthE2ETest {
  @Autowired private TestRestTemplate restTemplate;

  @MockitoSpyBean private AuthService authService;

  private String login(String providerId) {
    TestLoginRequest request = TestLoginRequest.create(providerId);
    ResponseEntity<Map> response = restTemplate.postForEntity("/auth/test", request, Map.class);
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return (String) data.get("accessToken");
  }

  private String getRefreshToken(String providerId) {
    TestLoginRequest request = TestLoginRequest.create(providerId);
    ResponseEntity<Map> response = restTemplate.postForEntity("/auth/test", request, Map.class);
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return (String) data.get("refreshToken");
  }

  private HttpHeaders createAuthHeader(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  @Nested
  @DisplayName("POST /auth/test - 테스트 로그인")
  class TestLogin {
    @Test
    @DisplayName("신규 회원 로그인 성공")
    void successNewMember() {
      // given
      String uniqueProviderId = "new-provider-" + System.currentTimeMillis();
      TestLoginRequest request = TestLoginRequest.create(uniqueProviderId);

      // when
      ResponseEntity<Map> response = restTemplate.postForEntity("/auth/test", request, Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> body = response.getBody();
      assertThat(body.get("success")).isEqualTo(true);

      Map<String, Object> data = (Map<String, Object>) body.get("data");
      assertThat(data.get("accessToken")).isNotNull();
      assertThat(data.get("refreshToken")).isNotNull();
      assertThat(data.get("newMember")).isEqualTo(true);
      System.out.println("data = " + data);
    }

    @Test
    @DisplayName("기존 회원 로그인 성공")
    void successExistingMember() {
      // given
      String existingProviderId = "1";
      TestLoginRequest request = TestLoginRequest.create(existingProviderId);

      // when
      ResponseEntity<Map> response = restTemplate.postForEntity("/auth/test", request, Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> body = response.getBody();
      assertThat(body.get("success")).isEqualTo(true);

      Map<String, Object> data = (Map<String, Object>) body.get("data");
      assertThat(data.get("accessToken")).isNotNull();
      assertThat(data.get("refreshToken")).isNotNull();
      assertThat(data.get("newMember")).isEqualTo(false);
      System.out.println("data = " + data);
    }
  }

  @Nested
  @DisplayName("POST /auth/logout - 로그아웃")
  class LogOut {
    @Test
    @DisplayName("로그아웃 성공")
    void success() {
      // given
      String accessToken = login("1");
      HttpHeaders headers = createAuthHeader(accessToken);

      // when
      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("토큰 없이 요청하면 401")
    void unauthorizedWithoutToken() {
      // when
      ResponseEntity<Map> response = restTemplate.postForEntity("/auth/logout", null, Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /auth/refresh - 토큰 갱신")
  class Refresh {
    @Test
    @DisplayName("토큰 갱신 성공")
    void success() {
      // given
      String refreshToken = getRefreshToken("1");
      AuthTokenRefreshRequest request = AuthTokenRefreshRequest.create(refreshToken);

      // when
      ResponseEntity<Map> response =
          restTemplate.postForEntity("/auth/refresh", request, Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> body = response.getBody();
      assertThat(body.get("success")).isEqualTo(true);

      Map<String, Object> data = (Map<String, Object>) body.get("data");
      assertThat(data.get("accessToken")).isNotNull();
      assertThat(data.get("refreshToken")).isNotNull();
    }

    @Test
    @DisplayName("잘못된 리프레시 토큰 401")
    void unauthorizedWithInvalidToken() {
      // given
      AuthTokenRefreshRequest request = AuthTokenRefreshRequest.create("invalid-refresh-token");

      // when
      ResponseEntity<Map> response =
          restTemplate.postForEntity("/auth/refresh", request, Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("POST /auth/google - Google 로그인")
  class GoogleLogin {
    @Test
    @DisplayName("성공")
    void success() {
      // given
      GoogleTokenResponse tokenResponse =
          GoogleTokenResponse.create("mock-access-token", "mock", 1234);
      GoogleUserInfo userInfo =
          GoogleUserInfo.create(
              "google-provider-" + System.currentTimeMillis(),
              "test@email.com",
              "user-" + System.currentTimeMillis(),
              "picture-url");

      Mockito.doReturn(tokenResponse).when(authService).getAccessToken(Mockito.anyString());
      Mockito.doReturn(userInfo).when(authService).getUserInfo(Mockito.anyString());

      GoogleLoginRequest request = GoogleLoginRequest.create("mock-auth-code");

      // when
      ResponseEntity<Map> response = restTemplate.postForEntity("/auth/google", request, Map.class);

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<String, Object> body = response.getBody();
      assertThat(body.get("success")).isEqualTo(true);

      Map<String, Object> data = (Map<String, Object>) body.get("data");
      assertThat(data.get("accessToken")).isNotNull();
      assertThat(data.get("refreshToken")).isNotNull();
      assertThat(data.get("email")).isEqualTo("test@email.com");
    }
  }
}
