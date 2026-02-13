package com.typingpractice.typing_practice_be.auth.service;

import com.typingpractice.typing_practice_be.auth.GoogleOAuthProperties;
import com.typingpractice.typing_practice_be.auth.JwtProperties;
import com.typingpractice.typing_practice_be.auth.dto.TokenRotation;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleLoginRequest;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleTokenResponse;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleUserInfo;
import com.typingpractice.typing_practice_be.auth.exception.AuthInvalidRefreshTokenException;
import com.typingpractice.typing_practice_be.auth.exception.GoogleAuthException;
import com.typingpractice.typing_practice_be.auth.exception.GoogleServerException;
import com.typingpractice.typing_practice_be.auth.repository.JwtBlackListRepository;
import com.typingpractice.typing_practice_be.auth.repository.RefreshTokenRepository;
import com.typingpractice.typing_practice_be.common.jwt.JwtPayload;
import com.typingpractice.typing_practice_be.common.jwt.JwtTokenProvider;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;

import java.time.Duration;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final RestClient restClient = RestClient.create();
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtBlackListRepository jwtBlackListRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  private final GoogleOAuthProperties googleOAuthProperties;
  private final JwtProperties jwtProperties;

  private final MemberRepository memberRepository;

  public GoogleTokenResponse getAccessToken(GoogleLoginRequest request) {
    try {
      String code = request.getCode();
      String redirectUri =
          request.getRedirectUri() != null
              ? request.getRedirectUri()
              : googleOAuthProperties.getRedirectUri();

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("code", code);
      params.add("client_id", googleOAuthProperties.getClientId());
      params.add("client_secret", googleOAuthProperties.getClientSecret());
      params.add("redirect_uri", redirectUri);
      params.add("grant_type", googleOAuthProperties.getGrantType());

      return restClient
          .post()
          .uri(googleOAuthProperties.getTokenUri())
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(params)
          .retrieve()
          .body(GoogleTokenResponse.class);
    } catch (HttpClientErrorException e) {
      log.warn(e.getMessage());

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

    refreshTokenRepository.deleteByMemberId(jwtPayload.getMemberId());

    jwtBlackListRepository.save(jwtPayload.getJwtId(), jwtPayload.getExpiresIn());
  }

  public boolean isBlacklistedJwt(String jwtId) {
    return jwtBlackListRepository.existByJwtId(jwtId);
  }

  @Transactional
  public TokenRotation rotateToken(String refreshToken) {
    // refresh token 으로 member id 조회
    Long memberId =
        refreshTokenRepository
            .findMemberIdByToken(refreshToken)
            .orElseThrow(AuthInvalidRefreshTokenException::new);

    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    String newAccessToken = jwtTokenProvider.createToken(memberId, member.getRole());

    refreshTokenRepository.deleteByToken(refreshToken); // 기존 리프레시 토큰 삭제
    String newRefreshToken = UUID.randomUUID().toString();
    Duration ttl = Duration.ofDays(jwtProperties.getRefreshTokenExpirationDays());
    refreshTokenRepository.save(memberId, newRefreshToken, ttl);

    return TokenRotation.create(newAccessToken, newRefreshToken);
  }

  @Transactional
  public String createRefreshToken(Member member) {
    // 기존 refresh token 삭제
    refreshTokenRepository.deleteByMemberId(member.getId());

    String token = UUID.randomUUID().toString();
    Duration ttl = Duration.ofDays(jwtProperties.getRefreshTokenExpirationDays());
    refreshTokenRepository.save(member.getId(), token, ttl);
    return token;
  }
}
