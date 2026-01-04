package com.typingpractice.typing_practice_be.member.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.query.MemberCreateQuery;
import com.typingpractice.typing_practice_be.member.exception.DuplicateEmailException;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.query.MemberLoginQuery;
import com.typingpractice.typing_practice_be.member.query.MemberUpdateQuery;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
  private final MemberRepository memberRepository;

  public Member findMemberById(Long memberId) {
    return memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
  }

  @Transactional
  public Member loginOrSignIn(MemberLoginQuery query) {
    Optional<Member> byEmail = memberRepository.findByEmail(query.getEmail());

    if (byEmail.isPresent()) {
      return memberRepository
          .login(query.getEmail(), query.getPassword())
          .orElseThrow(MemberNotFoundException::new);
    } else {
      MemberCreateQuery memberCreateQuery =
          MemberCreateQuery.of(query.getEmail(), query.getPassword(), Member.DEFAULT_NICKNAME);
      return this.join(memberCreateQuery);
    }
  }

  private Member join(MemberCreateQuery memberCreateQuery) {
    Member member =
        Member.createMember(
            memberCreateQuery.getEmail(),
            memberCreateQuery.getPassword(),
            memberCreateQuery.getNickname());

    if (memberRepository.findByEmail(memberCreateQuery.getEmail()).isPresent()) {
      throw new DuplicateEmailException();
    }

    memberRepository.save(member);

    return member;
  }

  @Transactional
  public Member updateNickname(Long memberId, MemberUpdateQuery query) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    String nickname = query.getNickname();

    // 변경하지 않은 경우 그냥 반환
    if (member.getNickname().equals(nickname)) {
      return member;
    }

    member.updateNickName(nickname);

    return member;
  }

  @Transactional
  public void deleteMember(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    memberRepository.deleteMember(member);
  }
}
