package com.typingpractice.typing_practice_be.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberOrderBy;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberBanRequest;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberPaginationRequest;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.exception.MemberNotProcessableException;
import com.typingpractice.typing_practice_be.member.query.MemberBanQuery;
import com.typingpractice.typing_practice_be.member.query.MemberPaginationQuery;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {
  @Mock private MemberRepository memberRepository;

  @InjectMocks private AdminMemberService adminMemberService;

  private Member createMember(MemberRole role) {
    Member member = Member.createMember("test@test.com", "password", "testMember");
    if (role != MemberRole.USER) {
      member.updateRole(role);
    }
    return member;
  }

  private MemberPaginationQuery createPaginationQuery() {
    MemberPaginationRequest request =
        new MemberPaginationRequest(1, 10, SortDirection.DESC, null, MemberOrderBy.id);

    return MemberPaginationQuery.from(request);
  }

  @Nested
  @DisplayName("findAllMembers")
  class FindAllMembers {

    @Test
    @DisplayName("회원 목록 조회 성공")
    void success() {
      // given
      Member member1 = createMember(MemberRole.USER);
      Member member2 = createMember(MemberRole.USER);
      MemberPaginationQuery query = createPaginationQuery();

      when(memberRepository.findAll(query)).thenReturn(List.of(member1, member2));
      // when
      List<Member> result = adminMemberService.findAllMembers(query);

      // then
      assertThat(result).hasSize(2);
    }
  }

  @Nested
  @DisplayName("findMemberById")
  class FindMemberById {
    @Test
    @DisplayName("회원 조회 성공")
    void success() {
      // given
      Member member = createMember(MemberRole.USER);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

      // when
      Member result = adminMemberService.findMemberById(1L);

      // then
      assertThat(result).isEqualTo(member);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // given
      when(memberRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> adminMemberService.findMemberById(1L))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("updateRole")
  class UpdateRole {
    @Test
    @DisplayName("역할 변경 성공")
    void success() {
      // given
      Member member = createMember(MemberRole.USER);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      // when
      Member result = adminMemberService.updateRole(1L, MemberRole.ADMIN);

      // then
      assertThat(result.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // given
      when(memberRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> adminMemberService.updateRole(1L, MemberRole.ADMIN))
          .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("ADMIN 의 역할 변경 시도 - 예외 발생")
    void adminNotProcessable() {
      // given
      Member admin = createMember(MemberRole.ADMIN);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(admin));

      // when & then
      assertThatThrownBy(() -> adminMemberService.updateRole(1L, MemberRole.BANNED))
          .isInstanceOf(MemberNotProcessableException.class);
    }
  }

  @Nested
  @DisplayName("banMember")
  class BanMember {
    @Test
    @DisplayName("회원 밴 성공")
    void success() {
      // given
      Member member = createMember(MemberRole.USER);
      MemberBanRequest request = MemberBanRequest.create("스팸 활동");
      MemberBanQuery query = MemberBanQuery.from(request);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

      // when
      Member result = adminMemberService.banMember(1L, query);

      // then
      assertThat(result.getRole()).isEqualTo(MemberRole.BANNED);
      assertThat(result.getBanReason()).isEqualTo("스팸 활동");
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // given
      MemberBanRequest request = MemberBanRequest.create("스팸 활동");
      MemberBanQuery query = MemberBanQuery.from(request);

      when(memberRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> adminMemberService.banMember(1L, query))
          .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("ADMIN 밴 시도 - 예외 발생")
    void adminNotProcessable() {
      // given
      Member admin = createMember(MemberRole.ADMIN);
      MemberBanRequest request = MemberBanRequest.create("스팸 활동");
      MemberBanQuery query = MemberBanQuery.from(request);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(admin));

      // when & then
      assertThatThrownBy(() -> adminMemberService.banMember(1L, query))
          .isInstanceOf(MemberNotProcessableException.class);
    }
  }

  @Nested
  @DisplayName("unbanMember")
  class UnbanMember {
    @Test
    @DisplayName("밴 해제 성공")
    void success() {
      // given
      Member member = createMember(MemberRole.BANNED);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      // when
      Member result = adminMemberService.unbanMember(1L);

      // then
      assertThat(result.getRole()).isEqualTo(MemberRole.USER);
      assertThat(result.getBanReason()).isEqualTo("");
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void notFound() {
      // given
      when(memberRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> adminMemberService.unbanMember(1L))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }
}
