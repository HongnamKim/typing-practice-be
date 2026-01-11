package com.typingpractice.typing_practice_be.auth;

import com.typingpractice.typing_practice_be.auth.dto.*;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.jwt.JwtTokenProvider;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.LoginResult;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    String token =
        jwtTokenProvider.createToken(member.getId(), member.getEmail(), member.getRole());

    return ApiResponse.ok(LoginResponse.from(loginResult, token));
  }

  @PostMapping("/google")
  public ApiResponse<LoginResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
    GoogleTokenResponse accessTokenResponse = authService.getAccessToken(request.getCode());
    String accessToken = accessTokenResponse.getAccessToken();

    GoogleUserInfo userInfo = authService.getUserInfo(accessToken);

    LoginResult loginResult = memberService.loginOrSignIn(userInfo);

    Member member = loginResult.getMember();
    String token =
        jwtTokenProvider.createToken(member.getId(), member.getEmail(), member.getRole());

    return ApiResponse.ok(LoginResponse.from(loginResult, token));
  }
}
