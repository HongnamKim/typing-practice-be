package com.typingpractice.typing_practice_be.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.auth.domain.JwtBlackList;
import com.typingpractice.typing_practice_be.auth.repository.JwtBlackListRepository;
import com.typingpractice.typing_practice_be.common.jwt.JwtPayload;
import com.typingpractice.typing_practice_be.common.jwt.JwtTokenProvider;
import java.time.LocalDateTime;
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

  @InjectMocks private AuthService authService;

  @Nested
  @DisplayName("logout")
  class Logout {
    @Test
    @DisplayName("로그아웃 성공 - 토큰이 블랙리스트에 저장됨")
    void success() {
      // given
      String token = "test-token";
      JwtPayload jwtPayload = JwtPayload.create(1L, "jwt-id-123", LocalDateTime.now().plusHours(1));

      when(jwtTokenProvider.getJwtPayload(token)).thenReturn(jwtPayload);

      // when
      authService.logout(token);

      // then
      verify(jwtBlackListRepository).save(any(JwtBlackList.class));
    }
  }

  @Nested
  @DisplayName("isBlacklistedJwt")
  class IsBlacklistedJwt {
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
}
