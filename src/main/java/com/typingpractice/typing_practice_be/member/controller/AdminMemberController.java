package com.typingpractice.typing_practice_be.member.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.*;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberBanRequest;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberPaginationRequest;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberPaginationResponse;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberUpdateRoleRequest;
import com.typingpractice.typing_practice_be.member.query.MemberBanQuery;
import com.typingpractice.typing_practice_be.member.query.MemberPaginationQuery;
import com.typingpractice.typing_practice_be.member.service.AdminMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
// @AdminOnly
public class AdminMemberController {
  private final AdminMemberService adminMemberService;

  @GetMapping("/admin/members")
  public ApiResponse<MemberPaginationResponse> getMemberList(
      @ModelAttribute @Valid MemberPaginationRequest request) {

    MemberPaginationQuery query = MemberPaginationQuery.from(request);

    PageResult<Member> result = adminMemberService.findAllMembers(query);

    return ApiResponse.ok(MemberPaginationResponse.from(result));
  }

  @GetMapping("/admin/members/{memberId}")
  public ApiResponse<MemberResponse> findMemberById(@PathVariable Long memberId) {

    Member member = adminMemberService.findMemberById(memberId);

    return ApiResponse.ok(MemberResponse.from(member));
  }

  @PatchMapping("/admin/members/{memberId}/role")
  public ApiResponse<MemberResponse> updateMemberRole(
      @PathVariable Long memberId, @RequestBody MemberUpdateRoleRequest request) {

    Member member = adminMemberService.updateRole(memberId, request.getRole());

    return ApiResponse.ok(MemberResponse.from(member));
  }

  @PostMapping("/admin/members/{memberId}/ban")
  public ApiResponse<MemberResponse> banMember(
      @PathVariable Long memberId, @RequestBody MemberBanRequest request) {

    MemberBanQuery query = MemberBanQuery.from(request);

    Member member = adminMemberService.banMember(memberId, query);

    return ApiResponse.ok(MemberResponse.from(member));
  }

  @PostMapping("/admin/members/{memberId}/unban")
  public ApiResponse<MemberResponse> unbanMember(@PathVariable Long memberId) {

    Member member = adminMemberService.unbanMember(memberId);

    return ApiResponse.ok(MemberResponse.from(member));
  }
}
