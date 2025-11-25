package com.typingpractice.typing_practice_be.member.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.LoginDto;
import com.typingpractice.typing_practice_be.member.dto.MemberResponseDto;
import com.typingpractice.typing_practice_be.member.dto.UpdateNicknameDto;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {
  private final MemberService memberService;

  @PostMapping("/login")
  public ApiResponse<MemberResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
    Member member = this.memberService.loginOrSignIn(loginDto);

    return ApiResponse.ok(MemberResponseDto.from(member));
  }

  @GetMapping
  public ApiResponse<List<MemberResponseDto>> memberList() {
    List<Member> members = memberService.findAllMembers();
    return ApiResponse.ok(members.stream().map(MemberResponseDto::from).toList());
  }

  @GetMapping("/{memberId}")
  public ApiResponse<MemberResponseDto> findMember(@PathVariable(name = "memberId") Long memberId) {
    Member member = memberService.findMemberById(memberId);

    return ApiResponse.ok(MemberResponseDto.from(member));
  }

  @PatchMapping("/{memberId}")
  public ApiResponse<MemberResponseDto> updateNickname(
      @PathVariable Long memberId, @RequestBody UpdateNicknameDto updateNicknameDto) {
    Member member = memberService.updateNickname(memberId, updateNicknameDto.getNickname());

    return ApiResponse.ok(MemberResponseDto.from(member));
  }

  @DeleteMapping("/{memberId}")
  public void deleteMember(@PathVariable Long memberId) {
    memberService.deleteMember(memberId);
  }
}
