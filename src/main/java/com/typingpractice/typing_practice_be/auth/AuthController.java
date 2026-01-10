package com.typingpractice.typing_practice_be.auth;

import com.typingpractice.typing_practice_be.auth.dto.GoogleLoginRequest;
import com.typingpractice.typing_practice_be.auth.dto.GoogleTokenResponse;
import com.typingpractice.typing_practice_be.auth.dto.GoogleUserInfo;
import com.typingpractice.typing_practice_be.common.ApiResponse;
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

  @PostMapping("/google")
  public ApiResponse<GoogleUserInfo> googleLogin(@RequestBody GoogleLoginRequest request) {
    GoogleTokenResponse accessTokenResponse = authService.getAccessToken(request.getCode());
    String accessToken = accessTokenResponse.getAccessToken();

    return ApiResponse.ok(authService.getUserInfo(accessToken));
  }
}
