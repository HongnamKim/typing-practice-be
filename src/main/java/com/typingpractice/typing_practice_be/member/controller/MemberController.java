package com.typingpractice.typing_practice_be.member.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.jwt.JwtTokenProvider;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.LoginDto;
import com.typingpractice.typing_practice_be.member.dto.LoginResponseDto;
import com.typingpractice.typing_practice_be.member.dto.MemberResponseDto;
import com.typingpractice.typing_practice_be.member.dto.UpdateNicknameDto;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {
  private final MemberService memberService;
  private final JwtTokenProvider jwtTokenProvider;

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
    Member member = this.memberService.loginOrSignIn(loginDto);

    String token = jwtTokenProvider.createToken(member.getId(), member.getEmail());

    return ApiResponse.ok(LoginResponseDto.of(member, token));
  }

  @GetMapping("/me")
  public ApiResponse<MemberResponseDto> findMember() {
    Long memberId = getAuthenticatedMemberId();

    Member member = memberService.findMemberById(memberId);

    return ApiResponse.ok(MemberResponseDto.from(member));
  }

  @PatchMapping("/me")
  public ApiResponse<MemberResponseDto> updateNickname(
      @RequestBody UpdateNicknameDto updateNicknameDto) {
    Long memberId = getAuthenticatedMemberId();
    Member member = memberService.updateNickname(memberId, updateNicknameDto.getNickname());

    return ApiResponse.ok(MemberResponseDto.from(member));
  }

  @DeleteMapping("/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ApiResponse<Void> deleteMember() {
    Long memberId = getAuthenticatedMemberId();

    memberService.deleteMember(memberId);
    return ApiResponse.ok(null);
  }

  private Long getAuthenticatedMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
