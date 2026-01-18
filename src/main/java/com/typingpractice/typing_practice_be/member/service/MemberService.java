package com.typingpractice.typing_practice_be.member.service;

import com.typingpractice.typing_practice_be.auth.dto.google.GoogleUserInfo;
import com.typingpractice.typing_practice_be.auth.repository.RefreshTokenRepository;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.LoginResult;
import com.typingpractice.typing_practice_be.member.exception.DuplicateNicknameException;
import com.typingpractice.typing_practice_be.member.query.MemberCreateQuery;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
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
  private final RefreshTokenRepository refreshTokenRepository;

  public Member findMemberById(Long memberId) {
    return memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
  }

  @Transactional
  public LoginResult loginOrSignIn(GoogleUserInfo googleUserInfo) {
    Optional<Member> optionalMember =
        memberRepository.findByProviderId(googleUserInfo.getProviderId());

    // 가입 완료한 회원
    if (optionalMember.isPresent()) {
      Member member = optionalMember.get();

      return LoginResult.create(member, false);
    } else {
      // 신규 회원
      MemberCreateQuery memberCreateQuery =
          MemberCreateQuery.of(
              googleUserInfo.getProviderId(), googleUserInfo.getEmail(), googleUserInfo.getName());

      Member member = join(memberCreateQuery);
      return LoginResult.create(member, true);
    }
  }

  private Member join(MemberCreateQuery memberCreateQuery) {
    Member member =
        Member.createMember(
            memberCreateQuery.getProviderId(),
            memberCreateQuery.getEmail(),
            memberCreateQuery.getNickname());

    memberRepository.save(member);

    return member;
  }

  public boolean checkNicknameDuplicated(String nickname) {
    return memberRepository.existByNickname(nickname);
  }

  @Transactional
  public Member updateNickname(Long memberId, MemberUpdateQuery query) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    String nickname = query.getNickname();

    // 변경하지 않은 경우 그냥 반환
    if (member.getNickname().equals(nickname)) {
      return member;
    }

    if (memberRepository.existByNickname(nickname)) {
      throw new DuplicateNicknameException();
    }

    member.updateNickName(nickname);

    return member;
  }

  @Transactional
  public void deleteMember(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

    refreshTokenRepository.deleteByMemberId(memberId);

    memberRepository.deleteMember(member);
  }
}
