package com.typingpractice.typing_practice_be.member.service;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.LoginDto;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MockMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class MemberServiceTest {

  private MockMemberRepository mockRepository;
  private MemberService memberService;

  @BeforeEach
  void setUp() {
    mockRepository = new MockMemberRepository();
    memberService = new MemberService(mockRepository);
  }

  @AfterEach
  void tearDown() {
    mockRepository.clear();
  }

  @Test
  void findMemberById() {
    // given
    LoginDto loginDto = LoginDto.create("email", "password");
    Member member = memberService.loginOrSignIn(loginDto);

    // when
    Member findMember = memberService.findMemberById(member.getId());

    // then
    assertThat(findMember).isEqualTo(member);
  }

  @Test
  void findAllMembers() {
    // given
    LoginDto loginA = LoginDto.create("memberA", "a");
    LoginDto loginB = LoginDto.create("memberB", "b");

    memberService.loginOrSignIn(loginA);
    memberService.loginOrSignIn(loginB);

    // when
    List<Member> allMembers = memberService.findAllMembers();

    // then
    assertThat(allMembers.size()).isEqualTo(2);
  }

  @Test
  void loginOrSignIn() {
    // given
    LoginDto loginDto = LoginDto.create("memberA", "A");
    Member memberA = memberService.loginOrSignIn(loginDto); // 회원가입

    // when
    Member loginMember = memberService.loginOrSignIn(loginDto);

    // then
    // assertThat(loginMember.getEmail()).isEqualTo(loginDto.getEmail());
    assertThat(loginMember).isEqualTo(memberA);
    List<Member> allMembers = memberService.findAllMembers();
    assertThat(allMembers.size()).isEqualTo(1);
  }

  @Test
  void updateNickname() {
    // given
    LoginDto loginDto = LoginDto.create("memberA", "a");
    Member memberA = memberService.loginOrSignIn(loginDto); // 회원가입

    // when
    memberService.updateNickname(memberA.getId(), "new_nickname");
    Member findMember = memberService.findMemberById(memberA.getId());

    // then
    assertThat(findMember.getNickname()).isEqualTo("new_nickname");
  }

  @Test
  void deleteMember() {
    // given
    LoginDto loginDto = LoginDto.create("memberA", "a");
    Member memberA = memberService.loginOrSignIn(loginDto);

    // when
    memberService.deleteMember(memberA.getId());

    // then
    assertThatThrownBy(() -> memberService.findMemberById(memberA.getId()))
        .isInstanceOf(MemberNotFoundException.class);
  }
}
