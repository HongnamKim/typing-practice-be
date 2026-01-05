package com.typingpractice.typing_practice_be.member.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.dto.*;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberBanRequest;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberPaginationRequest;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberPaginationResponse;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberUpdateRoleRequest;
import com.typingpractice.typing_practice_be.member.exception.NotAdminException;
import com.typingpractice.typing_practice_be.member.query.MemberBanQuery;
import com.typingpractice.typing_practice_be.member.query.MemberPaginationQuery;
import com.typingpractice.typing_practice_be.member.service.AdminMemberService;
import com.typingpractice.typing_practice_be.member.service.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdminMemberController {
  private final MemberService memberService;
  private final AdminMemberService adminMemberService;

  private void validateAdmin() {
    Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    Member admin = memberService.findMemberById(memberId);

    if (admin.getRole() != MemberRole.ADMIN) {
      throw new NotAdminException();
    }
  }

  @GetMapping("/admin/members")
  public ApiResponse<MemberPaginationResponse> getMemberList(
      @ModelAttribute @Valid MemberPaginationRequest request) {
    validateAdmin();

    MemberPaginationQuery query = MemberPaginationQuery.from(request);

    List<Member> allMembers = adminMemberService.findAllMembers(query);

    boolean hasNext = allMembers.size() > request.getSize();

    return ApiResponse.ok(
        MemberPaginationResponse.from(allMembers, request.getPage(), request.getSize(), hasNext));
  }

  @GetMapping("/admin/members/{memberId}")
  public ApiResponse<MemberResponse> findMemberById(@PathVariable Long memberId) {
    validateAdmin();

    Member member = adminMemberService.findMemberById(memberId);

    return ApiResponse.ok(MemberResponse.from(member));
  }

  @PatchMapping("/admin/members/{memberId}/role")
  public ApiResponse<MemberResponse> updateMemberRole(
      @PathVariable Long memberId, @RequestBody MemberUpdateRoleRequest request) {
    validateAdmin();

    Member member = adminMemberService.updateRole(memberId, request.getRole());

    return ApiResponse.ok(MemberResponse.from(member));
  }

  @PostMapping("/admin/members/{memberId}/ban")
  public ApiResponse<MemberResponse> banMember(
      @PathVariable Long memberId, @RequestBody MemberBanRequest request) {
    validateAdmin();

    MemberBanQuery query = MemberBanQuery.from(request);

    Member member = adminMemberService.banMember(memberId, query);

    return ApiResponse.ok(MemberResponse.from(member));
  }

  @PostMapping("/admin/members/{memberId}/unban")
  public ApiResponse<MemberResponse> unbanMember(@PathVariable Long memberId) {
    validateAdmin();

    Member member = adminMemberService.unbanMember(memberId);

    return ApiResponse.ok(MemberResponse.from(member));
  }
}
