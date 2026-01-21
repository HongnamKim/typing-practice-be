package com.typingpractice.typing_practice_be.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/problem+json;charset=UTF-8");

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    boolean isBanned =
        auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(MemberRole.BANNED.getAuthority()));

    ErrorCode errorCode = isBanned ? ErrorCode.MEMBER_BANNED : ErrorCode.NOT_ADMIN;

    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
    pd.setTitle("Forbidden");

    response.getWriter().write(objectMapper.writeValueAsString(pd));
  }
}
