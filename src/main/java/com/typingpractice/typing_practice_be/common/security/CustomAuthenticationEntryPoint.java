package com.typingpractice.typing_practice_be.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typingpractice.typing_practice_be.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper objectMapper;

  public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/problem+json;charset=UTF-8");

    ErrorCode errorCode = ErrorCode.UNAUTHORIZED_ERROR;

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
    problemDetail.setTitle("Unauthorized");

    response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
  }
}
