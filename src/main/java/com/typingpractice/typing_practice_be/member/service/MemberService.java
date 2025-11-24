package com.typingpractice.typing_practice_be.member.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.CreateMemberDto;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
  private final MemberRepository memberRepository;

  public Member join(CreateMemberDto createMemberDto) {
    Member member =
        Member.createMember(
            createMemberDto.getEmail(),
            createMemberDto.getPassword(),
            createMemberDto.getNickname());

    if (memberRepository.findByEmail(createMemberDto.getEmail()).isPresent()) {
      throw new IllegalStateException("이미 가입된 이메일입니다.");
    }

    memberRepository.save(member);

    return member;
  }
}
