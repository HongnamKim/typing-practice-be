package com.typingpractice.typing_practice_be.member.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.query.MemberPaginationQuery;
import java.util.List;
import java.util.Optional;

public interface MemberRepository {
  List<Member> findAll(MemberPaginationQuery query);

  Optional<Member> findById(Long memberId);

  Optional<Member> findByProviderId(String providerId);

  Long save(Member member);

  void deleteMember(Member member);

  boolean existByNickname(String nickname);
}
