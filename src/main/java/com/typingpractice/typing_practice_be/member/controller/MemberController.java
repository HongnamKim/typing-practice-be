package com.typingpractice.typing_practice_be.member.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.*;
import com.typingpractice.typing_practice_be.member.query.MemberUpdateQuery;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {
  private final MemberService memberService;

  @GetMapping("/me")
  public ApiResponse<MemberResponse> findMember() {
    Long memberId = getAuthenticatedMemberId();

    Member member = memberService.findMemberById(memberId);

    return ApiResponse.ok(MemberResponse.from(member));
  }

  @PatchMapping("/me")
  public ApiResponse<MemberResponse> updateNickname(
      @RequestBody UpdateNicknameRequest updateNicknameRequest) {
    Long memberId = getAuthenticatedMemberId();

    MemberUpdateQuery query = MemberUpdateQuery.from(updateNicknameRequest);

    Member member = memberService.updateNickname(memberId, query);

    return ApiResponse.ok(MemberResponse.from(member));
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
