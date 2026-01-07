package com.typingpractice.typing_practice_be.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportOrderBy;
import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.dto.ReportPaginationRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportProcessRequest;
import com.typingpractice.typing_practice_be.report.exception.ReportNotFoundException;
import com.typingpractice.typing_practice_be.report.query.ReportPaginationQuery;
import com.typingpractice.typing_practice_be.report.query.ReportProcessQuery;
import com.typingpractice.typing_practice_be.report.repository.ReportRepository;
import java.lang.reflect.Field;
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
class AdminReportServiceTest {
  @Mock private MemberRepository memberRepository;
  @Mock private QuoteRepository quoteRepository;
  @Mock private ReportRepository reportRepository;

  @InjectMocks private AdminReportService adminReportService;

  private Member createMember(Long id) {
    Member member = Member.createMember("test@test.com", "password", "testMember");
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

  private Quote createQuote() {
    Quote quote = Quote.create(createMember(99L), "테스트 문장", "저자", QuoteType.PUBLIC);
    quote.approvePublish();
    return quote;
  }

  private Quote createQuoteWithReportCount(int reportCount) {
    Quote quote = createQuote();
    for (int i = 0; i < reportCount; i++) {
      quote.increaseReportCount();
    }

    return quote;
  }

  private ReportPaginationQuery createPaginationQuery(Long memberId) {
    ReportPaginationRequest request =
        new ReportPaginationRequest(1, 10, SortDirection.DESC, null, ReportOrderBy.id, memberId);

    return ReportPaginationQuery.from(request);
  }

  private ReportProcessQuery createProcessQuery(String sentence, String author) {
    ReportProcessRequest request = ReportProcessRequest.create(sentence, author);
    return ReportProcessQuery.from(request);
  }

  @Nested
  @DisplayName("findReports")
  class FindReports {
    @Test
    @DisplayName("목록 조회 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote();
      Report report = Report.create(member, quote, ReportReason.MODIFY, "수정 요청");
      ReportPaginationQuery query = createPaginationQuery(null);

      when(reportRepository.findAll(query)).thenReturn(List.of(report));
      // when
      List<Report> reports = adminReportService.findReports(query);

      // then
      assertThat(reports).hasSize(1);
      verify(memberRepository, never()).findById(any());
    }

    @Test
    @DisplayName("memberId 필터 조회 성공")
    void successWithMemberId() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote();
      Report report = Report.create(member, quote, ReportReason.MODIFY, "수정 요청");
      ReportPaginationQuery query = createPaginationQuery(1L);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(reportRepository.findAll(query)).thenReturn(List.of(report));
      // when
      List<Report> result = adminReportService.findReports(query);

      // then
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 memberId - 예외 발생")
    void memberNotFound() {
      // given
      ReportPaginationQuery query = createPaginationQuery(999L);

      when(memberRepository.findById(999L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> adminReportService.findReports(query))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("processReport")
  class ProcessReport {
    @Test
    @DisplayName("문장 수정으로 처리 - 모든 신고 상태 PROCESSED")
    void successWithUpdate() {
      // given
      Quote quote = createQuoteWithReportCount(3);
      Member member1 = createMember(1L);
      Member member2 = createMember(2L);
      Report report1 = Report.create(member1, quote, ReportReason.MODIFY, "수정 요청");
      Report report2 = Report.create(member2, quote, ReportReason.DELETE, "삭제 요청");
      ReportProcessQuery query = createProcessQuery("수정된 문장", "수정된 저자");

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
      when(reportRepository.findByQuote(quote)).thenReturn(List.of(report1, report2));

      // when
      adminReportService.processReport(1L, query);

      // then
      assertThat(quote.getSentence()).isEqualTo("수정된 문장"); // 문장 수정 확인
      assertThat(quote.getAuthor()).isEqualTo("수정된 저자"); // 저자 수정 확인
      assertThat(quote.getReportCount()).isEqualTo(0); // 누적 신고 횟수 초기화 확인
      assertThat(report1.getStatus()).isEqualTo(ReportStatus.PROCESSED);
      assertThat(report1.isQuoteDeleted()).isFalse();
      assertThat(report2.getStatus()).isEqualTo(ReportStatus.PROCESSED);
      assertThat(report2.isQuoteDeleted()).isFalse();
    }

    @Test
    @DisplayName("문장 삭제로 처리 - 모든 신고 상태 PROCESSED, quoteDeleted true")
    void successWithDelete() {
      // given
      Quote quote = createQuote();
      Member member1 = createMember(1L);
      Member member2 = createMember(2L);
      Report report1 = Report.create(member1, quote, ReportReason.MODIFY, "수정 요청");
      Report report2 = Report.create(member2, quote, ReportReason.DELETE, "삭제 요청");
      ReportProcessQuery query = createProcessQuery(null, null); // 수정 내용이 없으면 삭제

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
      when(reportRepository.findByQuote(quote)).thenReturn(List.of(report1, report2));

      // when
      adminReportService.processReport(1L, query);

      // then
      verify(quoteRepository).deleteQuote(quote);
      assertThat(report1.getStatus()).isEqualTo(ReportStatus.PROCESSED);
      assertThat(report1.isQuoteDeleted()).isTrue();
      assertThat(report2.getStatus()).isEqualTo(ReportStatus.PROCESSED);
      assertThat(report2.isQuoteDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 예외 발생")
    void quoteNotFound() {
      // given
      ReportProcessQuery query = createProcessQuery("수정된 문장", "수정된 저자");

      when(quoteRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> adminReportService.processReport(1L, query))
          .isInstanceOf(QuoteNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("deleteReport")
  class DeleteReport {
    @Test
    @DisplayName("처리 미완료 신고 삭제 - reportCount 감소")
    void successNotProcessed() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuoteWithReportCount(3);
      Report report = Report.create(member, quote, ReportReason.MODIFY, "수정 요청");

      when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

      // when
      adminReportService.deleteReport(1L);

      // then
      assertThat(quote.getReportCount()).isEqualTo(2);
      verify(reportRepository).delete(report);
    }

    @Test
    @DisplayName("처리 완료 신고 삭제 - reportCount 유지")
    void successProcessed() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuoteWithReportCount(3);
      Report report = Report.create(member, quote, ReportReason.MODIFY, "modify");
      report.process(false); // 처리 완료 상태

      when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
      // when
      adminReportService.deleteReport(1L);

      // then
      assertThat(quote.getReportCount()).isEqualTo(3);
      verify(reportRepository).delete(report);
    }

    @Test
    @DisplayName("존재하지 않는 신고 - 예외 발생")
    void reportNotFound() {
      // given
      when(reportRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> adminReportService.deleteReport(1L))
          .isInstanceOf(ReportNotFoundException.class);
    }
  }
}
