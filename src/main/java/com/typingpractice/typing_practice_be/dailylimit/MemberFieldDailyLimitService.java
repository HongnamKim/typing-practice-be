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

  @Override
  public boolean canReport(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
    return member.canReportToday(DailyLimitPolicy.MAX_REPORT);
  }

  @Override
  public void incrementReportCount(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
    member.incrementReportCount();
  }
}
