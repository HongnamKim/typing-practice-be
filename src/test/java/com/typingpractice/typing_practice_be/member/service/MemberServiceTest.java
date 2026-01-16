package com.typingpractice.typing_practice_be.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.auth.dto.google.GoogleUserInfo;
import com.typingpractice.typing_practice_be.auth.repository.RefreshTokenRepository;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.LoginResult;
import com.typingpractice.typing_practice_be.member.dto.UpdateNicknameRequest;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.query.MemberUpdateQuery;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  @Mock private MemberRepository memberRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private MemberService memberService;

  private Member createMember(Long id, String providerId) {
    Member member = Member.createMember(providerId, "test@test.com", "testName");
    setId(member, id);
    return member;
  }

  private void setId(Object entity, Long id) {
    try {
      Field idField = entity.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
      Member member = createMember(1L, "provider-1");
      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

      // when
      Member findMember = memberService.findMemberById(1L);

      // then
      assertThat(findMember).isEqualTo(member);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // given
      when(memberRepository.findById(999L)).thenReturn(Optional.empty());

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
      when(memberRepository.findByProviderId("new-provider-id")).thenReturn(Optional.empty());

      // when
      LoginResult result = memberService.loginOrSignIn(userInfo);

      // then
      assertThat(result.isNewMember()).isTrue();
      assertThat(result.getMember().getProviderId()).isEqualTo("new-provider-id");
      verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("기존 회원 - 로그인 후 isNewMember false")
    void existingMember() {
      // given
      Member existingMember = createMember(1L, "provider-1");
      GoogleUserInfo userInfo = createGoogleUserInfo("provider-1");
      when(memberRepository.findByProviderId("provider-1")).thenReturn(Optional.of(existingMember));

      // when
      LoginResult result = memberService.loginOrSignIn(userInfo);

      // then
      assertThat(result.isNewMember()).isFalse();
      assertThat(result.getMember().getProviderId()).isEqualTo("provider-1");
      verify(memberRepository, never()).save(any(Member.class));
    }
  }

  @Nested
  @DisplayName("updateNickname")
  class UpdateNickname {
    @Test
    @DisplayName("닉네임 변경 성공")
    void success() {
      // given
      Member member = createMember(1L, "provider-1");
      UpdateNicknameRequest request = UpdateNicknameRequest.create("new_nickname");
      MemberUpdateQuery query = MemberUpdateQuery.from(request);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

      // when
      Member updated = memberService.updateNickname(1L, query);

      // then
      assertThat(updated.getNickname()).isEqualTo("new_nickname");
    }

    @Test
    @DisplayName("동일한 닉네임으로 변경 - 그대로 반환")
    void sameNickname() {
      // given
      Member member = createMember(1L, "provider-1");
      UpdateNicknameRequest request = UpdateNicknameRequest.create(member.getNickname());
      MemberUpdateQuery query = MemberUpdateQuery.from(request);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

      // when
      Member updated = memberService.updateNickname(1L, query);

      // then
      assertThat(updated.getNickname()).isEqualTo(member.getNickname());
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // given
      UpdateNicknameRequest request = UpdateNicknameRequest.create("new_nickname");
      MemberUpdateQuery query = MemberUpdateQuery.from(request);

      when(memberRepository.findById(999L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> memberService.updateNickname(999L, query))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("deleteMember")
  class DeleteMember {
    @Test
    @DisplayName("회원 삭제 성공 - RefreshToken도 함께 삭제")
    void success() {
      // given
      Member member = createMember(1L, "provider-1");
      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

      // when
      memberService.deleteMember(1L);

      // then
      verify(refreshTokenRepository).deleteByMemberId(1L);
      verify(memberRepository).deleteMember(member);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // given
      when(memberRepository.findById(999L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> memberService.deleteMember(999L))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }
}
