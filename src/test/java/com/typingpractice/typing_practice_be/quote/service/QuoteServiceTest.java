package com.typingpractice.typing_practice_be.quote.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.dailylimit.DailyLimitService;
import com.typingpractice.typing_practice_be.dailylimit.exception.DailyQuoteUploadLimitException;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.exception.MemberNotFoundException;
import com.typingpractice.typing_practice_be.member.repository.MemberRepository;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteOrderBy;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.PublicQuoteRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuoteCreateRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuotePaginationRequest;
import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotOwnedException;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotProcessableException;
import com.typingpractice.typing_practice_be.quote.query.PublicQuoteQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteCreateQuery;
import com.typingpractice.typing_practice_be.quote.query.QuotePaginationQuery;
import com.typingpractice.typing_practice_be.quote.query.QuoteUpdateQuery;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
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
class QuoteServiceTest {
  @Mock private QuoteRepository quoteRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private DailyLimitService dailyLimitService;

  @InjectMocks private QuoteService quoteService;

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

  private Quote createQuote(Member member, QuoteType type, QuoteStatus status) {
    Quote quote = Quote.create(member, "테스트 문장입니다.", "저자", type);
    if (type == QuoteType.PUBLIC && status == QuoteStatus.ACTIVE) {
      quote.approvePublish();
    }

    return quote;
  }

  private QuoteCreateQuery createCreateQuery(QuoteType type) {
    QuoteCreateRequest request = QuoteCreateRequest.create("테스트 문장입니다.", "저자", type);
    return QuoteCreateQuery.from(request);
  }

  private QuoteUpdateQuery createUpdateQuery(String sentence, String author) {
    QuoteUpdateRequest request = QuoteUpdateRequest.create(sentence, author);
    return QuoteUpdateQuery.from(request);
  }

  private QuotePaginationQuery createPaginationQuery() {
    QuotePaginationRequest request =
        new QuotePaginationRequest(1, 10, SortDirection.DESC, null, null, QuoteOrderBy.id);
    return QuotePaginationQuery.from(request);
  }

  @Nested
  @DisplayName("findById")
  class FindById {
    @Test
    @DisplayName("조회 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PRIVATE, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when
      Quote result = quoteService.findById(1L);

      // then
      assertThat(result).isEqualTo(quote);
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 예외 발생")
    void notFound() {
      // given
      when(quoteRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> quoteService.findById(1L))
          .isInstanceOf(QuoteNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {
    @Test
    @DisplayName("PRIVATE 문장 생성 성공")
    void successPrivate() {
      // given
      Member member = createMember(1L);
      QuoteCreateQuery query = createCreateQuery(QuoteType.PRIVATE);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(dailyLimitService.canUploadQuote(1L)).thenReturn(true);

      // when
      Quote result = quoteService.create(1L, query);

      // then
      assertThat(result.getType()).isEqualTo(QuoteType.PRIVATE);
      assertThat(result.getStatus()).isEqualTo(QuoteStatus.ACTIVE);
      verify(quoteRepository).save(any(Quote.class));
      verify(dailyLimitService).incrementQuoteUploadCount(1L);
    }

    @Test
    @DisplayName("PUBLIC 문장 생성 성공 - PENDING 상태")
    void successPublic() {
      // given
      Member member = createMember(1L);
      QuoteCreateQuery query = createCreateQuery(QuoteType.PUBLIC);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(dailyLimitService.canUploadQuote(1L)).thenReturn(true);

      // when
      Quote result = quoteService.create(1L, query);

      // then
      assertThat(result.getType()).isEqualTo(QuoteType.PUBLIC);
      assertThat(result.getStatus()).isEqualTo(QuoteStatus.PENDING);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void memberNotFound() {
      // given
      QuoteCreateQuery query = createCreateQuery(QuoteType.PUBLIC);

      when(memberRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> quoteService.create(1L, query))
          .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("일일 업로드 제한 초과 - 예외 발생")
    void dailyLimitExceeded() {
      // given
      Member member = createMember(1L);
      QuoteCreateQuery query = createCreateQuery(QuoteType.PUBLIC);

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(dailyLimitService.canUploadQuote(1L)).thenReturn(false);

      // when & then
      assertThatThrownBy(() -> quoteService.create(1L, query))
          .isInstanceOf(DailyQuoteUploadLimitException.class);
    }
  }

  @Nested
  @DisplayName("updatePrivateQuote")
  class UpdatePrivateQuote {
    @Test
    @DisplayName("수정 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PRIVATE, QuoteStatus.ACTIVE);
      QuoteUpdateQuery query = createUpdateQuery("수정된 문장입니다.", "수정된 저자");

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when
      Quote result = quoteService.updatePrivateQuote(1L, 1L, query);

      // then
      assertThat(result.getSentence()).isEqualTo("수정된 문장입니다.");
      assertThat(result.getAuthor()).isEqualTo("수정된 저자");
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 예외 발생")
    void quoteNotFound() {
      // given
      QuoteUpdateQuery query = createUpdateQuery("수정된 문장", "수정된 저자");

      when(quoteRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> quoteService.updatePrivateQuote(1L, 1L, query))
          .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    @DisplayName("본인 문장이 아님 - 예외 발생")
    void notOwned() {
      // given
      Member otherMember = createMember(2L);
      Quote quote = createQuote(otherMember, QuoteType.PRIVATE, QuoteStatus.ACTIVE);
      QuoteUpdateQuery query = createUpdateQuery("수정된 문장", "수정된 저자");

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> quoteService.updatePrivateQuote(1L, 1L, query))
          .isInstanceOf(QuoteNotOwnedException.class);
    }

    @Test
    @DisplayName("PUBLIC 문장 수정 시도 - 예외 발생")
    void publicNotProcessable() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PUBLIC, QuoteStatus.ACTIVE);
      QuoteUpdateQuery query = createUpdateQuery("수정된 문장", "수정된 저자");

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> quoteService.updatePrivateQuote(1L, 1L, query))
          .isInstanceOf(QuoteNotProcessableException.class);
    }
  }

  @Nested
  @DisplayName("deleteQuote")
  class DeleteQuote {
    @Test
    @DisplayName("삭제 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PRIVATE, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when
      quoteService.deleteQuote(1L, 1L);

      // then
      verify(quoteRepository).deleteQuote(quote);
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 예외 발생")
    void quoteNotFound() {
      // given
      when(quoteRepository.findById(1L)).thenReturn(Optional.empty());

      // when & given
      assertThatThrownBy(() -> quoteService.deleteQuote(1L, 1L))
          .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    @DisplayName("본인 문장이 아님 - 예외 발생")
    void notOwned() {
      // given
      Member otherMember = createMember(2L);
      Quote quote = createQuote(otherMember, QuoteType.PRIVATE, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> quoteService.deleteQuote(1L, 1L))
          .isInstanceOf(QuoteNotOwnedException.class);
    }

    @Test
    @DisplayName("PUBLIC 문장 삭제 시도 - 예외 발생")
    void publicNotProcessable() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PUBLIC, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> quoteService.deleteQuote(1L, 1L))
          .isInstanceOf(QuoteNotProcessableException.class);
    }
  }

  @Nested
  @DisplayName("getMyQuotes")
  class GetMyQuotes {
    @Test
    @DisplayName("조회 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PRIVATE, QuoteStatus.ACTIVE);
      QuotePaginationQuery query = createPaginationQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
      when(quoteRepository.findByMember(member, query)).thenReturn(List.of(quote));

      // when
      List<Quote> result = quoteService.getMyQuotes(1L, query);

      // then
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 회원 - 예외 발생")
    void memberNotFound() {
      // given
      QuotePaginationQuery query = createPaginationQuery();

      when(memberRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> quoteService.getMyQuotes(1L, query))
          .isInstanceOf(MemberNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("publishQuote")
  class PublishQuote {
    @Test
    @DisplayName("공개 전환 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PRIVATE, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when
      Quote result = quoteService.publishQuote(1L, 1L);

      // then
      assertThat(result.getType()).isEqualTo(QuoteType.PUBLIC);
      assertThat(result.getStatus()).isEqualTo(QuoteStatus.PENDING);
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 예외 발생")
    void quoteNotFound() {
      // given
      when(quoteRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> quoteService.publishQuote(1L, 1L))
          .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    @DisplayName("본인 문장이 아님 - 예외 발생")
    void notOwned() {
      // given
      Member otherMember = createMember(2L);
      Quote quote = createQuote(otherMember, QuoteType.PRIVATE, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> quoteService.publishQuote(1L, 1L))
          .isInstanceOf(QuoteNotOwnedException.class);
    }

    @Test
    @DisplayName("이미 PUBLIC 문장 - 예외 발생")
    void alreadyPublic() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PUBLIC, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> quoteService.publishQuote(1L, 1L))
          .isInstanceOf(QuoteNotProcessableException.class);
    }
  }

  @Nested
  @DisplayName("cancelPublishQuote")
  class CancelPublishQuote {
    @Test
    @DisplayName("공개 전환 취소 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PUBLIC, QuoteStatus.PENDING);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
      // when
      Quote result = quoteService.cancelPublishQuote(1L, 1L);

      // then
      assertThat(result.getType()).isEqualTo(QuoteType.PRIVATE);
      assertThat(result.getStatus()).isEqualTo(QuoteStatus.ACTIVE);
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 예외 발생")
    void quoteNotFound() {
      // given
      when(quoteRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> quoteService.cancelPublishQuote(1L, 1L))
          .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    @DisplayName("본인 문장이 아님 - 예외 발생")
    void notOwned() {
      // given
      Member otherMember = createMember(2L);
      Quote quote = createQuote(otherMember, QuoteType.PUBLIC, QuoteStatus.PENDING);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> quoteService.cancelPublishQuote(1L, 1L))
          .isInstanceOf(QuoteNotOwnedException.class);
    }

    @Test
    @DisplayName("PENDING 상태가 아님 - 예외 발생")
    void notPending() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PUBLIC, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> quoteService.cancelPublishQuote(1L, 1L))
          .isInstanceOf(QuoteNotProcessableException.class);
    }

    @Test
    @DisplayName("PRIVATE 문장 취소 시도 - 예외 발생")
    void privateNotProcessable() {
      // given
      Member member = createMember(1L);
      Quote quote = createQuote(member, QuoteType.PRIVATE, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> quoteService.cancelPublishQuote(1L, 1L))
          .isInstanceOf(QuoteNotProcessableException.class);
    }
  }

  @Nested
  @DisplayName("findRandomPublicQuotes")
  class FindRandomPublicQuotes {

    @Test
    @DisplayName("조회 성공")
    void success() {
      // given
      Member member = createMember(1L);
      Quote quote1 = createQuote(member, QuoteType.PUBLIC, QuoteStatus.ACTIVE);
      Quote quote2 = createQuote(member, QuoteType.PUBLIC, QuoteStatus.ACTIVE);
      PublicQuoteRequest request = new PublicQuoteRequest(1, 10, false, 0.5f);
      PublicQuoteQuery query = PublicQuoteQuery.from(1L, request);

      when(quoteRepository.findPublicQuotes(query)).thenReturn(List.of(quote1, quote2));

      // when
      List<Quote> result = quoteService.findRandomPublicQuotes(query);

      // then
      assertThat(result).hasSize(2);
      verify(quoteRepository).findPublicQuotes(query);
    }
  }
}
