package com.typingpractice.typing_practice_be.member.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;

import java.util.Optional;

public interface MemberRepository {
  Optional<Member> findById(Long memberId);

  Optional<Member> findByEmail(String email);

  Long save(Member member);

  void deleteMember(Member member);
}
