package com.typingpractice.typing_practice_be.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.dailylimit.DailyLimitService;
import com.typingpractice.typing_practice_be.dailylimit.exception.DailyReportLimitException;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportOrderBy;
import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.dto.ReportCreateRequest;
import com.typingpractice.typing_practice_be.report.dto.ReportPaginationRequest;
import com.typingpractice.typing_practice_be.report.exception.DuplicateReportException;
import com.typingpractice.typing_practice_be.report.exception.QuoteNotReportableException;
import com.typingpractice.typing_practice_be.report.exception.ReportNotFoundException;
import com.typingpractice.typing_practice_be.report.exception.ReportNotProcessableException;
import com.typingpractice.typing_practice_be.report.query.ReportCreateQuery;
import com.typingpractice.typing_practice_be.report.query.ReportPaginationQuery;
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
class ReportServiceTest {
  @Mock private MemberRepository memberRepository;
  @Mock private QuoteRepository quoteRepository;
  @Mock private ReportRepository reportRepository;
  @Mock private DailyLimitService dailyLimitService;

  @InjectMocks private ReportService reportService;

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

  private Quote createQuote(QuoteType type, QuoteStatus status) {
    Quote quote = Quote.create(createMember(99L), "테스트 문장", "저자", type);
    if (type == QuoteType.PUBLIC && status == QuoteStatus.ACTIVE) {
      quote.approvePublish();
    }
    return quote;
  }

  private Quote createQuoteWithReportCount(int reportCount) {
    Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);
    for (int i = 0; i < reportCount; i++) {
      quote.increaseReportCount();
    }
    return quote;
  }

  private ReportCreateQuery createReportQuery() {
    ReportCreateRequest request = ReportCreateRequest.create(1L, ReportReason.MODIFY, "수정 요청");
    return ReportCreateQuery.from(request);
  }

  private ReportPaginationQuery createPaginationQuery() {
    ReportPaginationRequest request =
        new ReportPaginationRequest(1, 10, SortDirection.DESC, null, ReportOrderBy.id, null);
    return ReportPaginationQuery.from(request);
  }

  @Nested
  @DisplayName("createReport")
  class CreateReport {
    @Test
    @DisplayName("신고 생성 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);
      ReportCreateQuery query = createReportQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
      when(dailyLimitService.canReport(1L)).thenReturn(true);
      when(reportRepository.existsByQuoteAndMember(quote, member)).thenReturn(false);

      // when
      Report result = reportService.createReport(1L, 1L, query);

      // then
      assertThat(result.getReason()).isEqualTo(ReportReason.MODIFY);
      assertThat(result.getDetail()).isEqualTo("수정 요청");
      assertThat(quote.getReportCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("신고 누적으로 문장 숨김 처리")
    void hiddenByReportCount() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuoteWithReportCount(4); // 미리 신고된 횟수 4 + 새로운 신고 = 5개 -> 숨김처리
      ReportCreateQuery query = createReportQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
      when(dailyLimitService.canReport(1L)).thenReturn(true);
      when(reportRepository.existsByQuoteAndMember(quote, member)).thenReturn(false);
      // when
      reportService.createReport(1L, 1L, query);

      // then
      assertThat(quote.getReportCount()).isEqualTo(5);
      assertThat(quote.getStatus()).isEqualTo(QuoteStatus.HIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void memberNotFound() {
      // given
      ReportCreateQuery query = createReportQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> reportService.createReport(1L, 1L, query))
          .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 예외 발생")
    void quoteNotFound() {
      // given
      Member member = createMember(1L);
      ReportCreateQuery query = createReportQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(quoteRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> reportService.createReport(1L, 1L, query))
          .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    @DisplayName("신고 불가능한 문장 (PRIVATE) - 예외 발생")
    void privateQuoteNotReportable() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(QuoteType.PRIVATE, QuoteStatus.ACTIVE);
      ReportCreateQuery query = createReportQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> reportService.createReport(1L, 1L, query))
          .isInstanceOf(QuoteNotReportableException.class);
    }

    @Test
    @DisplayName("신고 불가능한 문장 (PENDING) - 예외 발생")
    void pendingQuoteNotReportable() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.PENDING);
      ReportCreateQuery query = createReportQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> reportService.createReport(1L, 1L, query))
          .isInstanceOf(QuoteNotReportableException.class);
    }

    @Test
    @DisplayName("일일 신고 제한 초과 - 예외 발생")
    void dailyLimitExceeded() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);
      ReportCreateQuery query = createReportQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
      when(dailyLimitService.canReport(1L)).thenReturn(false);

      // when & then
      assertThatThrownBy(() -> reportService.createReport(1L, 1L, query))
          .isInstanceOf(DailyReportLimitException.class);
    }

    @Test
    @DisplayName("중복 신고 - 에외 발생")
    void duplicateReport() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);
      ReportCreateQuery query = createReportQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
      when(dailyLimitService.canReport(1L)).thenReturn(true);
      when(reportRepository.existsByQuoteAndMember(quote, member)).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> reportService.createReport(1L, 1L, query))
          .isInstanceOf(DuplicateReportException.class);
    }
  }

  @Nested
  @DisplayName("findMyReports")
  class FindMyReports {
    @Test
    @DisplayName("내 신고 내역 조회 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);
      Report report = Report.create(member, quote, ReportReason.MODIFY, "수정 요청");
      ReportPaginationQuery query = createPaginationQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(reportRepository.findMyReports(member, query)).thenReturn(List.of(report));

      // when
      PageResult<Report> result = reportService.findMyReports(1L, query);

      // then
      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void memberNotFound() {
      // given
      ReportPaginationQuery query = createPaginationQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> reportService.findMyReports(1L, query))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("deleteReport")
  class DeleteReport {
    @Test
    @DisplayName("신고 삭제 성공 (처리 미완료 신고)")
    void successNotProcessed() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuoteWithReportCount(1);
      Report report = Report.create(member, quote, ReportReason.MODIFY, "수정 요청");
      ReportCreateQuery query = createReportQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
      when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
      when(dailyLimitService.canReport(1L)).thenReturn(true);
      when(reportRepository.existsByQuoteAndMember(quote, member)).thenReturn(false);

      // when
      reportService.createReport(1L, 1L, query);
      assertThat(quote.getReportCount()).isEqualTo(2);
      reportService.deleteReport(1L, 1L);

      // then
      assertThat(quote.getReportCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("신고 삭제 성공 (처리 완료 신고)")
    void successProcessed() throws NoSuchFieldException, IllegalAccessException {
      // given
      Member member = createMember(1L);
      Quote quote = createQuoteWithReportCount(1);
      Report report = Report.create(member, quote, ReportReason.MODIFY, "수정 요청");
      Field reportStatus = report.getClass().getDeclaredField("status");
      reportStatus.setAccessible(true);
      reportStatus.set(report, ReportStatus.PROCESSED);

      when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

      // when
      reportService.deleteReport(1L, 1L);

      // then
      assertThat(quote.getReportCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 신고 - 예외 발생")
    void reportNotFound() {
      // given
      when(reportRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> reportService.deleteReport(1L, 1L))
          .isInstanceOf(ReportNotFoundException.class);
    }

    @Test
    @DisplayName("본인 신고가 아님 - 예외 발생")
    void notOwnReport() {
      // given
      Member otherMember = createMember(2L);
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);
      Report report = Report.create(otherMember, quote, ReportReason.MODIFY, "수정 요청");

      when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

      // when & then
      assertThatThrownBy(() -> reportService.deleteReport(1L, 1L))
          .isInstanceOf(ReportNotProcessableException.class);
    }
  }
}
