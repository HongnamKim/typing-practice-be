package com.typingpractice.typing_practice_be.member.controller;

import com.typingpractice.typing_practice_be.common.ApiResponse;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.dto.*;
import com.typingpractice.typing_practice_be.member.exception.NotAdminException;
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
  public ApiResponse<MemberListResponse> getMemberList(
      @ModelAttribute @Valid MemberPaginationRequest request) {
    validateAdmin();

    List<Member> allMembers = adminMemberService.findAllMembers(request);

    boolean hasNext = allMembers.size() > request.getSize();

    List<MemberResponseDto> data =
        allMembers.stream().limit(request.getSize()).map(MemberResponseDto::from).toList();

    return ApiResponse.ok(
        new MemberListResponse(data, request.getPage(), request.getSize(), hasNext));
  }

  @GetMapping("/admin/members/{memberId}")
  public ApiResponse<MemberResponseDto> findMemberById(@PathVariable Long memberId) {
    validateAdmin();

    Member member = adminMemberService.findMemberById(memberId);

    return ApiResponse.ok(MemberResponseDto.from(member));
  }

  @PatchMapping("/admin/members/{memberId}/role")
  public ApiResponse<MemberResponseDto> updateMemberRole(
      @PathVariable Long memberId, @RequestBody UpdateMemberRoleRequest request) {
    validateAdmin();

    Member member = adminMemberService.updateRole(memberId, request.getRole());

    return ApiResponse.ok(MemberResponseDto.from(member));
  }

  @PostMapping("/admin/members/{memberId}/ban")
  public ApiResponse<MemberResponseDto> banMember(
      @PathVariable Long memberId, @RequestBody BanMemberRequest request) {
    validateAdmin();

    Member member = adminMemberService.banMember(memberId, request);

    return ApiResponse.ok(MemberResponseDto.from(member));
  }

  @PostMapping("/admin/members/{memberId}/unban")
  public ApiResponse<MemberResponseDto> unbanMember(@PathVariable Long memberId) {
    validateAdmin();

    Member member = adminMemberService.unbanMember(memberId);

    return ApiResponse.ok(MemberResponseDto.from(member));
  }
}
