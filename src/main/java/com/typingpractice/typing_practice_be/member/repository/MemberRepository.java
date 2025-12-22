package com.typingpractice.typing_practice_be.member.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberPaginationRequest;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {
  List<Member> findAll(MemberPaginationRequest request);

  Optional<Member> findById(Long memberId);

  Optional<Member> findByEmail(String email);

  Optional<Member> login(String email, String password);

  Long save(Member member);

  void deleteMember(Member member);
}
