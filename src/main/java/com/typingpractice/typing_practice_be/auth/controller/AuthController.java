package com.typingpractice.typing_practice_be.auth.controller;

import com.typingpractice.typing_practice_be.auth.dto.google.GoogleLoginRequest;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleTokenResponse;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleUserInfo;
import com.typingpractice.typing_practice_be.auth.service.AuthService;
import com.typingpractice.typing_practice_be.auth.dto.*;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.jwt.JwtTokenProvider;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.LoginResult;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final MemberService memberService;
  private final JwtTokenProvider jwtTokenProvider;

  @PostMapping("/test")
  public ApiResponse<LoginResponse> testLogin(@RequestBody TestLoginRequest request) {

    LoginResult loginResult =
        memberService.loginOrSignIn(
            GoogleUserInfo.create(request.getProviderId(), "email", "name", "picture"));

    Member member = loginResult.getMember();

    String token = jwtTokenProvider.createToken(member.getId(), member.getRole());

    return ApiResponse.ok(LoginResponse.from(loginResult, token));
  }

  @PostMapping("/google")
  public ApiResponse<LoginResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
    GoogleTokenResponse accessTokenResponse = authService.getAccessToken(request.getCode());
    String accessToken = accessTokenResponse.getAccessToken();

    GoogleUserInfo userInfo = authService.getUserInfo(accessToken);

    LoginResult loginResult = memberService.loginOrSignIn(userInfo);

    Member member = loginResult.getMember();
    String token = jwtTokenProvider.createToken(member.getId(), member.getRole());

    return ApiResponse.ok(LoginResponse.from(loginResult, token));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {

    String token = authHeader.substring("Bearer ".length());

    authService.logout(token);

    return ApiResponse.ok(null);
  }
}
