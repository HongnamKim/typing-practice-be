package com.typingpractice.typing_practice_be.auth;

import com.typingpractice.typing_practice_be.auth.dto.GoogleTokenResponse;
import com.typingpractice.typing_practice_be.auth.dto.GoogleUserInfo;
import com.typingpractice.typing_practice_be.auth.exception.GoogleAuthException;
import com.typingpractice.typing_practice_be.auth.exception.GoogleServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AuthService {
  @Value("${google.client-id}")
  private String clientId;

  @Value("${google.client-secret}")
  private String clientSecret;

  @Value("${google.redirect-uri}")
  private String redirectUri;

  @Value("${google.token-uri}")
  private String tokenUri;

  private final RestClient restClient = RestClient.create();

  public GoogleTokenResponse getAccessToken(String code) {
    try {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("code", code);
      params.add("client_id", clientId);
      params.add("client_secret", clientSecret);
      params.add("redirect_uri", redirectUri);
      params.add("grant_type", "authorization_code");

      return restClient
          .post()
          .uri(tokenUri)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(params)
          .retrieve()
          .body(GoogleTokenResponse.class);
    } catch (HttpClientErrorException e) {
      throw new GoogleAuthException();
    } catch (HttpServerErrorException e) {
      throw new GoogleServerException();
    }
  }

  public GoogleUserInfo getUserInfo(String accessToken) {
    try {
      return restClient
          .get()
          .uri("https://www.googleapis.com/oauth2/v2/userinfo")
          .header("Authorization", "Bearer " + accessToken)
          .retrieve()
          .body(GoogleUserInfo.class);

    } catch (HttpClientErrorException e) {
      throw new GoogleAuthException();
    } catch (HttpServerErrorException e) {
      throw new GoogleServerException();
    }
  }
}
