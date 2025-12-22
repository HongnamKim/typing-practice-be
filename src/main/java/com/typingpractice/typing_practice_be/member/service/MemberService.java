package com.typingpractice.typing_practice_be.member.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.CreateMemberDto;
import com.typingpractice.typing_practice_be.member.dto.LoginRequest;
import com.typingpractice.typing_practice_be.member.exception.DuplicateEmailException;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
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
  public Member loginOrSignIn(LoginRequest loginRequest) {
    Optional<Member> byEmail = memberRepository.findByEmail(loginRequest.getEmail());

    if (byEmail.isPresent()) {

      return memberRepository
          .login(loginRequest.getEmail(), loginRequest.getPassword())
          .orElseThrow(MemberNotFoundException::new);
    } else {

      CreateMemberDto createMemberDto =
          CreateMemberDto.create(loginRequest.getEmail(), loginRequest.getPassword(), "nickname");
      return this.join(createMemberDto);
    }
  }

  private Member join(CreateMemberDto createMemberDto) {
    Member member =
        Member.createMember(
            createMemberDto.getEmail(),
            createMemberDto.getPassword(),
            createMemberDto.getNickname());

    if (memberRepository.findByEmail(createMemberDto.getEmail()).isPresent()) {
      throw new DuplicateEmailException();
    }

    memberRepository.save(member);

    return member;
  }

  @Transactional
  public Member updateNickname(Long memberId, String nickname) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

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
