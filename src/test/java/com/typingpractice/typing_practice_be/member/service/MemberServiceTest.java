package com.typingpractice.typing_practice_be.member.service;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.MemberLoginRequest;
import com.typingpractice.typing_practice_be.member.dto.UpdateNicknameRequest;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.query.MemberLoginQuery;
import com.typingpractice.typing_practice_be.member.query.MemberUpdateQuery;
import com.typingpractice.typing_practice_be.member.repository.MockMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    MemberLoginRequest memberLoginRequest = MemberLoginRequest.create("email", "password");
    MemberLoginQuery query = MemberLoginQuery.from(memberLoginRequest);
    Member member = memberService.loginOrSignIn(query);

    // when
    Member findMember = memberService.findMemberById(member.getId());

    // then
    assertThat(findMember).isEqualTo(member);
  }

  //  @Test
  //  void findAllMembers() {
  //    // given
  //    LoginDto loginA = LoginDto.create("memberA", "a");
  //    LoginDto loginB = LoginDto.create("memberB", "b");
  //
  //    memberService.loginOrSignIn(loginA);
  //    memberService.loginOrSignIn(loginB);
  //
  //    // when
  //    MemberPaginationRequest request = MemberPaginationRequest.create(1, 10, null);
  //    List<Member> allMembers = memberService.findAllMembers(request);
  //
  //    // then
  //    assertThat(allMembers.size()).isEqualTo(2);
  //  }

  @Test
  void loginOrSignIn() {
    // given
    MemberLoginRequest memberLoginRequest = MemberLoginRequest.create("memberA", "A");
    MemberLoginQuery query = MemberLoginQuery.from(memberLoginRequest);
    Member memberA = memberService.loginOrSignIn(query); // 회원가입

    // when
    Member loginMember = memberService.loginOrSignIn(query);

    // then
    // assertThat(loginMember.getEmail()).isEqualTo(loginDto.getEmail());
    assertThat(loginMember).isEqualTo(memberA);
    // List<Member> allMembers = memberService.findAllMembers();
    // assertThat(allMembers.size()).isEqualTo(1);
  }

  @Test
  void updateNickname() {
    // given
    MemberLoginRequest memberLoginRequest = MemberLoginRequest.create("memberA", "a");
    MemberLoginQuery query = MemberLoginQuery.from(memberLoginRequest);
    Member memberA = memberService.loginOrSignIn(query); // 회원가입

    // when
    UpdateNicknameRequest request = UpdateNicknameRequest.create("new_nickname");
    MemberUpdateQuery updateQuery = MemberUpdateQuery.from(request);
    memberService.updateNickname(memberA.getId(), updateQuery);
    Member findMember = memberService.findMemberById(memberA.getId());

    // then
    assertThat(findMember.getNickname()).isEqualTo("new_nickname");
  }

  @Test
  void deleteMember() {
    // given
    MemberLoginRequest memberLoginRequest = MemberLoginRequest.create("memberA", "a");
    MemberLoginQuery query = MemberLoginQuery.from(memberLoginRequest);
    Member memberA = memberService.loginOrSignIn(query);

    // when
    memberService.deleteMember(memberA.getId());

    // then
    assertThatThrownBy(() -> memberService.findMemberById(memberA.getId()))
        .isInstanceOf(MemberNotFoundException.class);
  }
}
