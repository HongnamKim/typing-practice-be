package com.typingpractice.typing_practice_be.auth.service;

import com.typingpractice.typing_practice_be.auth.domain.JwtBlackList;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleTokenResponse;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleUserInfo;
import com.typingpractice.typing_practice_be.common.jwt.JwtPayload;
import com.typingpractice.typing_practice_be.auth.exception.GoogleAuthException;
import com.typingpractice.typing_practice_be.auth.exception.GoogleServerException;
import com.typingpractice.typing_practice_be.auth.repository.JwtBlackListRepository;
import com.typingpractice.typing_practice_be.common.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtBlackListRepository jwtBlackListRepository;

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

  @Transactional
  public void logout(String token) {
    JwtPayload jwtPayload = jwtTokenProvider.getJwtPayload(token);

    JwtBlackList jwtBlackList =
        JwtBlackList.create(jwtPayload.getJwtId(), jwtPayload.getExpiresIn());

    jwtBlackListRepository.save(jwtBlackList);
  }

  public boolean isBlacklistedJwt(String jwtId) {
    return jwtBlackListRepository.existByJwtId(jwtId);
  }
}
