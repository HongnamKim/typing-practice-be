package com.typingpractice.typing_practice_be.member.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.dto.MemberPaginationRequest;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminMemberService {
  private final MemberRepository memberRepository;

  public List<Member> findAllMembers(MemberPaginationRequest request) {
    List<Member> members = memberRepository.findAll(request);

    return members;
  }

  public Member updateRole(Long memberId, MemberRole role) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    member.updateRole(role);

    return member;
  }
}
