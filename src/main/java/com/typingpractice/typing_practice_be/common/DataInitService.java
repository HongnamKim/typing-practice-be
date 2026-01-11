package com.typingpractice.typing_practice_be.common;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import com.typingpractice.typing_practice_be.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitService implements CommandLineRunner {
  private final MemberRepository memberRepository;
  private final QuoteRepository quoteRepository;
  private final ReportRepository reportRepository;

  @Override
  @Transactional
  public void run(String... args) {
    initAdmin();
    List<Member> members = initMembers();
    List<Quote> quotes = initQuotes(members);
    initReports(members, quotes);

    log.info("초기 데이터 생성 완료");
  }

  private void initAdmin() {
    Member admin = Member.createMember("0", "admin@admin.com", "admin");
    memberRepository.save(admin);

    admin.updateRole(MemberRole.ADMIN);

    log.info("어드민 계정 생성 완료");
  }

  private List<Member> initMembers() {
    List<Member> members = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      Member member = Member.createMember(String.valueOf(i), "user" + i + "@test.com", "유저" + i);
      memberRepository.save(member);
      members.add(member);
    }
    log.info("회원 {}명 생성 완료", members.size());

    return members;
  }

  private List<Quote> initQuotes(List<Member> members) {
    List<Quote> quotes = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      Member owner = members.get(i % 10);
      QuoteType type = i % 5 == 0 ? QuoteType.PRIVATE : QuoteType.PUBLIC;

      Quote quote = Quote.create(owner, "테스트 문장입니다. 번호: " + i, "작자 " + i, type);

      if (type == QuoteType.PUBLIC && i % 5 < 4) {
        quote.approvePublish();
      }

      quoteRepository.save(quote);
      quotes.add(quote);
    }

    log.info("문장 {}개 생성 완료", quotes.size());

    return quotes;
  }

  private void initReports(List<Member> members, List<Quote> quotes) {
    List<Quote> reportableQuotes =
        quotes.stream()
            .filter(q -> q.getType() == QuoteType.PUBLIC && q.getStatus() == QuoteStatus.ACTIVE)
            .toList();

    int reportCount = 0;
    for (int i = 0; i < 20 && i < reportableQuotes.size(); i++) {
      Quote quote = reportableQuotes.get(i);
      Member reporter = members.get((i + 1) % 10);

      Report report =
          Report.create(
              reporter,
              quote,
              i % 2 == 0 ? ReportReason.MODIFY : ReportReason.DELETE,
              "신고 상세 사유 " + i);

      reportRepository.save(report);
      quote.increaseReportCount();
      reportCount++;
    }
    log.info("신고 {}건 생성 완료", reportCount);
  }
}
