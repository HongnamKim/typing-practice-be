package com.typingpractice.typing_practice_be.common.jwt;

import com.typingpractice.typing_practice_be.auth.service.AuthService;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthService authService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    // 헤더에서 토큰 추출 (토큰 내용만)
    String token = resolveToken(request);

    if (token != null && jwtTokenProvider.validateToken(token)) {
      try {
        Long memberId = jwtTokenProvider.getMemberId(token);
        String role = jwtTokenProvider.getRole(token);
        MemberRole memberRole = MemberRole.valueOf(role);

        String jwtId = jwtTokenProvider.getJwtPayload(token).getJwtId();

        if (authService.isBlacklistedJwt(jwtId)) {
          SecurityContextHolder.clearContext();
        } else {
          // BANNED 포함 모든 Role 인증 설정
          List<SimpleGrantedAuthority> authorities =
              List.of(new SimpleGrantedAuthority(memberRole.getAuthority()));

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(memberId, null, authorities);

          // 인증된 내용을 SecurityContext 에 넣어서 컨트롤러에서 사용할 수 있게 함.
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }

      } catch (Exception e) {
        SecurityContextHolder.clearContext();
      }
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7); // 앞에 Bearer 제거
    }

    return null;
  }
}
