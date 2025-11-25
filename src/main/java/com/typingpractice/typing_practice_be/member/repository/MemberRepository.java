package com.typingpractice.typing_practice_be.member.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {
  List<Member> findAll();

  Optional<Member> findById(Long memberId);

  Optional<Member> findByEmail(String email);

  Optional<Member> login(String email, String password);

  Long save(Member member);

  void deleteMember(Member member);
}
