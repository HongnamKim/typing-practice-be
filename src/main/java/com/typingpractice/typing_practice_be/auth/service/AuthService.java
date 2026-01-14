package com.typingpractice.typing_practice_be.auth.service;

import com.typingpractice.typing_practice_be.auth.GoogleOAuthProperties;
import com.typingpractice.typing_practice_be.auth.JwtProperties;
import com.typingpractice.typing_practice_be.auth.domain.JwtBlackList;
import com.typingpractice.typing_practice_be.auth.domain.RefreshToken;
import com.typingpractice.typing_practice_be.auth.dto.TokenRotation;
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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

  private final RestClient restClient = RestClient.create();
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtBlackListRepository jwtBlackListRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  private final GoogleOAuthProperties googleOAuthProperties;
  private final JwtProperties jwtProperties;

  private final MemberRepository memberRepository;

  public GoogleTokenResponse getAccessToken(String code) {
    try {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("code", code);
      params.add("client_id", googleOAuthProperties.getClientId());
      params.add("client_secret", googleOAuthProperties.getClientSecret());
      params.add("redirect_uri", googleOAuthProperties.getRedirectUri());
      params.add("grant_type", googleOAuthProperties.getGrantType());

      return restClient
          .post()
          .uri(googleOAuthProperties.getTokenUri())
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

    refreshTokenRepository.deleteByMemberId(jwtPayload.getMemberId());

    jwtBlackListRepository.save(jwtBlackList);
  }

  public boolean isBlacklistedJwt(String jwtId) {
    return jwtBlackListRepository.existByJwtId(jwtId);
  }

  @Transactional
  public TokenRotation rotateToken(String refreshToken) {
    RefreshToken refreshTokenInfo =
        refreshTokenRepository
            .findByToken(refreshToken)
            .orElseThrow(AuthInvalidRefreshTokenException::new);

    if (refreshTokenInfo.getExpiresIn().isBefore(LocalDateTime.now())) {
      throw new AuthInvalidRefreshTokenException();
    }

    Long memberId = refreshTokenInfo.getMemberId();

    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    String newAccessToken = jwtTokenProvider.createToken(memberId, member.getRole());
    String newRefreshToken = UUID.randomUUID().toString();

    RefreshToken newRefreshTokenInfo =
        RefreshToken.create(
            memberId,
            newRefreshToken,
            LocalDateTime.now().plusDays(jwtProperties.getRefreshTokenExpirationDays()));

    refreshTokenRepository.deleteByToken(refreshToken); // 기존 토큰 삭제
    refreshTokenRepository.save(newRefreshTokenInfo);

    return TokenRotation.create(newAccessToken, newRefreshToken);
  }

  @Transactional
  public String createRefreshToken(Member member) {
    RefreshToken refreshToken =
        RefreshToken.create(
            member.getId(),
            UUID.randomUUID().toString(),
            LocalDateTime.now().plusDays(jwtProperties.getRefreshTokenExpirationDays()));
    refreshTokenRepository.save(refreshToken);
    return refreshToken.getToken();
  }
}
