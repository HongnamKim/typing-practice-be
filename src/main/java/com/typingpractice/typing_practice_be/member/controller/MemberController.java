package com.typingpractice.typing_practice_be.member.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.*;
import com.typingpractice.typing_practice_be.member.query.MemberUpdateQuery;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import com.typingpractice.typing_practice_be.typingrecord.statistics.domain.MemberTypingStats;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

  @GetMapping("/check-nickname")
  public ApiResponse<Boolean> checkNicknameDuplicated(
      @ModelAttribute @Valid CheckNicknameRequest request) {

    boolean isExist = memberService.checkNicknameDuplicated(request.getNickname());

    return ApiResponse.ok(isExist);
  }

  @PatchMapping("/me")
  public ApiResponse<MemberResponse> updateNickname(
      @RequestBody @Valid UpdateNicknameRequest updateNicknameRequest) {
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

  @GetMapping("/me/typing-profile")
  public ApiResponse<List<MemberTypingStats>> getTypingProfile() {
    Long memberId = getAuthenticatedMemberId();

    List<MemberTypingStats> memberTypingStats = memberService.findMemberTypingStats(memberId);

    return ApiResponse.ok(memberTypingStats);
  }

  private Long getAuthenticatedMemberId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
