package com.typingpractice.typing_practice_be.member.service;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.exception.MemberNotProcessableException;
import com.typingpractice.typing_practice_be.member.query.MemberBanQuery;
import com.typingpractice.typing_practice_be.member.query.MemberPaginationQuery;
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

  public PageResult<Member> findAllMembers(MemberPaginationQuery query) {
    List<Member> members = memberRepository.findAll(query);

    boolean hasNext = members.size() > query.getSize();
    List<Member> content = hasNext ? members.subList(0, query.getSize()) : members;

    return new PageResult<>(content, query.getPage(), query.getSize(), hasNext);
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
  public Member banMember(Long memberId, MemberBanQuery query) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    if (member.getRole() == MemberRole.ADMIN) {
      throw new MemberNotProcessableException();
    }

    member.ban(query.getBanReason());

    return member;
  }

  @Transactional
  public Member unbanMember(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    member.unban();

    return member;
  }
}
