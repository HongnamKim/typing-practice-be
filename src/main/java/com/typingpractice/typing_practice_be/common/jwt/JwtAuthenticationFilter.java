package com.typingpractice.typing_practice_be.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtTokenProvider jwtTokenProvider;

  //  @Override
  //  protected boolean shouldNotFilter(HttpServletRequest request) {
  //    String path = request.getRequestURI();
  //    return path.startsWith("/swagger-ui")
  //        || path.startsWith("/v3/api-docs")
  //        || path.startsWith("/h2-console");
  //  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // 헤더에서 토큰 추출 (토큰 내용만)
    String token = resolveToken(request);

    if (token != null && jwtTokenProvider.validateToken(token)) {
      Long memberId = jwtTokenProvider.getMemberId(token);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList());

      // 인증된 내용을 SecurityContext 에 넣어서 컨트롤러에서 사용할 수 있게 함.
      SecurityContextHolder.getContext().setAuthentication(authentication);
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
