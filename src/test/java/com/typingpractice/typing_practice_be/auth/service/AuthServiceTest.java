package com.typingpractice.typing_practice_be.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.auth.JwtProperties;
import com.typingpractice.typing_practice_be.auth.dto.TokenRotation;
import com.typingpractice.typing_practice_be.auth.exception.AuthInvalidRefreshTokenException;
import com.typingpractice.typing_practice_be.auth.repository.JwtBlackListRepository;
import com.typingpractice.typing_practice_be.auth.repository.RefreshTokenRepository;
import com.typingpractice.typing_practice_be.common.jwt.JwtPayload;
import com.typingpractice.typing_practice_be.common.jwt.JwtTokenProvider;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock private JwtBlackListRepository jwtBlackListRepository;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  // @Mock private GoogleOAuthProperties googleOAuthProperties;
  @Mock private JwtProperties jwtProperties;
  @Mock private MemberRepository memberRepository;

  @InjectMocks private AuthService authService;

  private Member createMember(Long id) {
    Member member = Member.createMember("test-provider-id", "test@test.com", "testMember");
    setId(member, id);
    return member;
  }

  private void setId(Object entity, Long id) {
    try {
      Field idField = entity.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nested
  @DisplayName("logout")
  class Logout {
    @Test
    @DisplayName("로그아웃 성공 - 블랙리스트 저장 및 RefreshToken 삭제")
    void success() {
      // given
      String token = "test-token";
      JwtPayload jwtPayload = JwtPayload.create(1L, "jwt-id-123", LocalDateTime.now().plusHours(1));

      when(jwtTokenProvider.getJwtPayload(token)).thenReturn(jwtPayload);

      // when
      authService.logout(token);

      // then
      verify(refreshTokenRepository).deleteByMemberId(1L);
      verify(jwtBlackListRepository).save(eq("jwt-id-123"), any(LocalDateTime.class));
    }
  }

  @Nested
  @DisplayName("isBlackListedJwt")
  class IsBlackListedJwt {
    @Test
    @DisplayName("블랙리스트에 없는 토큰 - false 반환")
    void notBlacklisted() {
      // given
      String jwtId = "not-exist-jwt-id";
      when(jwtBlackListRepository.existByJwtId(jwtId)).thenReturn(false);

      // when & then
      assertThat(authService.isBlacklistedJwt(jwtId)).isFalse();
    }

    @Test
    @DisplayName("블랙리스트에 있는 토큰 - true 반환")
    void blacklisted() {
      // given
      String jwtId = "exist-jwt-id";
      when(jwtBlackListRepository.existByJwtId(jwtId)).thenReturn(true);

      // when & then
      assertThat(authService.isBlacklistedJwt(jwtId)).isTrue();
    }
  }

  @Nested
  @DisplayName("rotateToken")
  class RotateToken {
    @Test
    @DisplayName("토큰 갱신 성공")
    void success() {
      // given
      String oldRefreshToken = "old-refresh-token";
      Long memberId = 1L;
      Member member = createMember(memberId);

      when(refreshTokenRepository.findMemberIdByToken(oldRefreshToken))
          .thenReturn(Optional.of(memberId));
      when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
      when(jwtTokenProvider.createToken(memberId, MemberRole.USER)).thenReturn("new-access-token");
      when(jwtProperties.getRefreshTokenExpirationDays()).thenReturn(15L);

      // when
      TokenRotation result = authService.rotateToken(oldRefreshToken);

      // then
      assertThat(result.getAccessToken()).isEqualTo("new-access-token");
      assertThat(result.getRefreshToken()).isNotEqualTo(oldRefreshToken);
      verify(refreshTokenRepository).deleteByToken(oldRefreshToken);
      verify(refreshTokenRepository).save(eq(memberId), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("존재하지 않는 RefreshToken - 예외 발생")
    void tokenInvalid() {
      // given
      String invalidToken = "invalid-token";
      when(refreshTokenRepository.findMemberIdByToken(invalidToken)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> authService.rotateToken(invalidToken))
          .isInstanceOf(AuthInvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void memberNotFound() {
      // given
      String refreshToken = "valid-token";
      Long memberId = 999L;

      when(refreshTokenRepository.findMemberIdByToken(refreshToken))
          .thenReturn(Optional.of(memberId));
      when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> authService.rotateToken(refreshToken))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("createRefreshToken")
  class CreateRefreshToken {
    @Test
    @DisplayName("RefreshToken 생성 성공 - 기존 토큰 삭제 후 생성")
    void success() {
      // given
      Member member = createMember(1L);
      when(jwtProperties.getRefreshTokenExpirationDays()).thenReturn(15L);

      // when
      String result = authService.createRefreshToken(member);

      // then
      assertThat(result).isNotNull();
      verify(refreshTokenRepository).deleteByMemberId(1L);
      verify(refreshTokenRepository).save(eq(1L), anyString(), any(Duration.class));
    }
  }
}
