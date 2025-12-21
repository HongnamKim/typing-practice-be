package com.typingpractice.typing_practice_be.member.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.dto.BanMemberRequest;
import com.typingpractice.typing_practice_be.member.dto.MemberPaginationRequest;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.exception.MemberNotProcessableException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {
  private final MemberRepository memberRepository;

  public List<Member> findAllMembers(MemberPaginationRequest request) {
    List<Member> members = memberRepository.findAll(request);

    return members;
  }

  public Member findMemberById(Long memberId) {
    return memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
  }

  @Transactional
  public Member updateRole(Long memberId, MemberRole role) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    if (member.getRole() == MemberRole.ADMIN) {
      throw new MemberNotProcessableException();
    }

    member.updateRole(role);

    return member;
  }

  @Transactional
  public Member banMember(Long memberId, BanMemberRequest request) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    if (member.getRole() == MemberRole.ADMIN) {
      throw new MemberNotProcessableException();
    }

    member.ban(request.getBanReason());

    return member;
  }

  @Transactional
  public Member unbanMember(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    member.unban();

    return member;
  }
}
