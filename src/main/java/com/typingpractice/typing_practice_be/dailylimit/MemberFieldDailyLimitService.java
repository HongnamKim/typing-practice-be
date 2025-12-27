package com.typingpractice.typing_practice_be.dailylimit;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class MemberFieldDailyLimitService implements DailyLimitService {
  private final MemberRepository memberRepository;

  private Member findMemberById(Long memberId) {
    return memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
  }

  @Override
  public boolean canReport(Long memberId) {
    Member member = findMemberById(memberId);
    return member.canReportToday(DailyLimitPolicy.MAX_REPORT);
  }

  @Override
  public void incrementReportCount(Long memberId) {
    Member member = findMemberById(memberId);
    member.incrementReportCount();
  }

  @Override
  public boolean canUploadQuote(Long memberId) {
    Member member = findMemberById(memberId);
    return member.canUploadQuote(DailyLimitPolicy.MAX_QUOTE_UPLOAD);
  }

  @Override
  public void incrementQuoteUploadCount(Long memberId) {
    Member member = findMemberById(memberId);
    member.incrementQuoteUploadCount();
  }
}
