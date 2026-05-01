package com.typingpractice.typing_practice_be.auth.controller;

import com.typingpractice.typing_practice_be.auth.dto.LoginResponse;
import com.typingpractice.typing_practice_be.auth.dto.TestLoginRequest;
import com.typingpractice.typing_practice_be.auth.dto.google.GoogleUserInfo;
import com.typingpractice.typing_practice_be.auth.service.AuthService;
import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.jwt.JwtTokenProvider;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.LoginResult;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("local")
public class TestAuthController {

  private final AuthService authService;
  private final MemberService memberService;
  private final JwtTokenProvider jwtTokenProvider;

  @PostMapping("/auth/test")
  public ApiResponse<LoginResponse> testLogin(@RequestBody TestLoginRequest request) {

    LoginResult loginResult =
        memberService.loginOrSignIn(
            GoogleUserInfo.create(
                request.getProviderId(),
                "email",
                "user_" + UUID.randomUUID().toString().substring(0, 8),
                "picture"));

    Member member = loginResult.getMember();

    String token = jwtTokenProvider.createToken(member.getId(), member.getRole());
    String refreshToken = authService.createRefreshToken(member);

    return ApiResponse.ok(LoginResponse.from(loginResult, token, refreshToken));
  }
}
