package com.typingpractice.typing_practice_be.member.service;

import static org.assertj.core.api.Assertions.*;

import com.typingpractice.typing_practice_be.auth.dto.google.GoogleUserInfo;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.LoginResult;
import com.typingpractice.typing_practice_be.member.dto.UpdateNicknameRequest;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.query.MemberUpdateQuery;
import com.typingpractice.typing_practice_be.member.repository.MockMemberRepository;
import org.junit.jupiter.api.*;

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

  private GoogleUserInfo createGoogleUserInfo(String providerId) {
    return GoogleUserInfo.create(providerId, "test@test.com", "testName", "picture");
  }

  @Nested
  @DisplayName("findMemberById")
  class FindMemberById {
    @Test
    @DisplayName("회원 조회 성공")
    void success() {
      // given
      GoogleUserInfo userInfo = createGoogleUserInfo("provider-1");
      LoginResult result = memberService.loginOrSignIn(userInfo);
      Member member = result.getMember();

      // when
      Member findMember = memberService.findMemberById(member.getId());

      // then
      assertThat(findMember).isEqualTo(member);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // when & then
      assertThatThrownBy(() -> memberService.findMemberById(999L))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("loginOrSignIn")
  class LoginOrSignIn {
    @Test
    @DisplayName("신규 회원 - 회원가입 후 isNewMember true")
    void newMember() {
      // given
      GoogleUserInfo userInfo = createGoogleUserInfo("new-provider-id");

      // when
      LoginResult result = memberService.loginOrSignIn(userInfo);

      // then
      assertThat(result.isNewMember()).isTrue();
      assertThat(result.getMember().getProviderId()).isEqualTo("new-provider-id");
    }

    @Test
    @DisplayName("기존 회원 - 로그인 후 isNewMember false")
    void existingMember() {
      // given
      GoogleUserInfo userInfo = createGoogleUserInfo("provider-1");
      memberService.loginOrSignIn(userInfo);

      // when
      LoginResult secondResult = memberService.loginOrSignIn(userInfo);

      // then
      assertThat(secondResult.isNewMember()).isFalse();
      assertThat(secondResult.getMember().getProviderId()).isEqualTo("provider-1");
    }
  }

  @Nested
  @DisplayName("updateNickname")
  class UpdateNickname {
    @Test
    @DisplayName("닉네임 변경 성공")
    void success() {
      // given
      GoogleUserInfo userInfo = createGoogleUserInfo("provider-1");
      LoginResult result = memberService.loginOrSignIn(userInfo);
      Member member = result.getMember();

      UpdateNicknameRequest request = UpdateNicknameRequest.create("new_nickname");
      MemberUpdateQuery query = MemberUpdateQuery.from(request);

      // when
      memberService.updateNickname(member.getId(), query);
      Member findMember = memberService.findMemberById(member.getId());

      // then
      assertThat(findMember.getNickname()).isEqualTo("new_nickname");
    }

    @Test
    @DisplayName("동일한 닉네임으로 변경 - 그대로 반환")
    void sameNickname() {
      // given
      GoogleUserInfo userInfo = createGoogleUserInfo("provider-1");
      LoginResult result = memberService.loginOrSignIn(userInfo);
      Member member = result.getMember();

      UpdateNicknameRequest request = UpdateNicknameRequest.create(member.getNickname());
      MemberUpdateQuery query = MemberUpdateQuery.from(request);

      // when
      Member updated = memberService.updateNickname(member.getId(), query);

      // then
      assertThat(updated.getNickname()).isEqualTo(member.getNickname());
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // given
      UpdateNicknameRequest request = UpdateNicknameRequest.create("new_nickname");
      MemberUpdateQuery query = MemberUpdateQuery.from(request);

      // when & then
      assertThatThrownBy(() -> memberService.updateNickname(999L, query))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("deleteMember")
  class DeleteMember {
    @Test
    @DisplayName("회원 삭제 성공")
    void success() {
      // given
      GoogleUserInfo userInfo = createGoogleUserInfo("provider-1");
      LoginResult result = memberService.loginOrSignIn(userInfo);
      Member member = result.getMember();

      // when
      memberService.deleteMember(member.getId());

      // then
      assertThatThrownBy(() -> memberService.findMemberById(member.getId()))
          .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // when & then
      assertThatThrownBy(() -> memberService.deleteMember(999L))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }
}
