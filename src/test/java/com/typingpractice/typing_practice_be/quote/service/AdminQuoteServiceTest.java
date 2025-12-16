package com.typingpractice.typing_practice_be.quote.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.QuoteUpdateRequest;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotFoundException;
import com.typingpractice.typing_practice_be.quote.exception.QuoteNotProcessableException;
import com.typingpractice.typing_practice_be.quote.repository.QuoteRepository;
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
class AdminQuoteServiceTest {
  @Mock private QuoteRepository quoteRepository;

  @InjectMocks private AdminQuoteService adminQuoteService;

  private Member createMember() {
    return Member.createMember("test@test.com", "password", "testMember");
  }

  private Quote createQuote(QuoteType type, QuoteStatus status) {
    Quote quote = Quote.create(createMember(), "테스트 문장입니다.", "작자", type);

    if (type == QuoteType.PUBLIC && status == QuoteStatus.ACTIVE) {
      quote.approvePublish();
    } else if (type == QuoteType.PUBLIC && status == QuoteStatus.HIDDEN) {
      quote.approvePublish();
      quote.updateStatus(QuoteStatus.HIDDEN);
    }

    return quote;
  }

  @Nested
  @DisplayName("findPendingQuotes")
  class FindPendingQuotes {
    @Test
    @DisplayName("승인 대기 목록 조회 성공")
    void success() {
      Quote quote1 = createQuote(QuoteType.PUBLIC, QuoteStatus.PENDING);
      Quote quote2 = createQuote(QuoteType.PUBLIC, QuoteStatus.PENDING);

      when(quoteRepository.findByStatus(QuoteStatus.PENDING)).thenReturn(List.of(quote1, quote2));

      List<Quote> result = adminQuoteService.findPendingQuotes();

      assertThat(result).hasSize(2);
      verify(quoteRepository).findByStatus(QuoteStatus.PENDING);
    }
  }

  @Nested
  @DisplayName("findHiddenQuotes")
  class FindHiddenQuotes {
    @Test
    @DisplayName("숨김 목록 조회 성공")
    void success() {
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.HIDDEN);

      when(quoteRepository.findByStatus(QuoteStatus.HIDDEN)).thenReturn(List.of(quote));

      // when
      List<Quote> result = adminQuoteService.findHiddenQuotes();

      // then
      assertThat(result).hasSize(1);
      verify(quoteRepository).findByStatus(QuoteStatus.HIDDEN);
    }
  }

  @Nested
  @DisplayName("approvePublish")
  class ApprovePublish {
    @Test
    @DisplayName("공개 승인 성공")
    void success() {
      // given
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.PENDING);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when
      Quote result = adminQuoteService.approvePublish(1L);

      // then
      assertThat(result.getStatus()).isEqualTo(QuoteStatus.ACTIVE);
      assertThat(result.getType()).isEqualTo(QuoteType.PUBLIC);
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 예외 발생")
    void notFound() {
      // given
      when(quoteRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> adminQuoteService.approvePublish(1L))
          .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    @DisplayName("PENDING 상태가 아닌 문장 - 예외 발생")
    void notPending() {
      // given
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> adminQuoteService.approvePublish(1L))
          .isInstanceOf(QuoteNotProcessableException.class);
    }

    @Test
    @DisplayName("PRIVATE 문장 - 예외 발생")
    void privateQuote() {
      // given
      Quote quote = createQuote(QuoteType.PRIVATE, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> adminQuoteService.approvePublish(1L))
          .isInstanceOf(QuoteNotProcessableException.class);
    }
  }

  @Nested
  @DisplayName("rejectPublish")
  class RejectPublish {
    @Test
    @DisplayName("공개 거부 성공")
    void success() {
      // given
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.PENDING);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when
      Quote result = adminQuoteService.rejectPublish(1L);

      // then
      assertThat(result.getStatus()).isEqualTo(QuoteStatus.ACTIVE);
      assertThat(result.getType()).isEqualTo(QuoteType.PRIVATE);
    }

    @Test
    @DisplayName("PENDING 상태가 아닌 문장 - 예외 발생")
    void notPending() {
      // given
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> adminQuoteService.rejectPublish(1L))
          .isInstanceOf(QuoteNotProcessableException.class);
    }
  }

  @Nested
  @DisplayName("updateQuote")
  class UpdateQuote {
    @Test
    @DisplayName("공개 문장 수정 성공")
    void success() {
      // given
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);
      String newSentence = "수정된 문장입니다.";
      String newAuthor = "수정된 저자";
      QuoteUpdateRequest request = QuoteUpdateRequest.create(newSentence, newAuthor);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
      // when
      Quote result = adminQuoteService.updateQuote(1L, request);

      // then
      assertThat(result.getSentence()).isEqualTo(newSentence);
      assertThat(result.getAuthor()).isEqualTo(newAuthor);
    }

    @Test
    @DisplayName("개인 문장 수정 시도 - 예외 발생")
    void privateQuote() {
      // given
      Quote quote = createQuote(QuoteType.PRIVATE, QuoteStatus.ACTIVE);
      String newSentence = "수정된 문장입니다.";
      String newAuthor = "수정된 저자";
      QuoteUpdateRequest request = QuoteUpdateRequest.create(newSentence, newAuthor);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> adminQuoteService.updateQuote(1L, request))
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
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when
      adminQuoteService.deleteQuote(1L);

      // then
      verify(quoteRepository).deleteQuote(quote);
    }

    @Test
    @DisplayName("존재하지 않는 문장 - 예외 발생")
    void notFound() {
      // given
      when(quoteRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> adminQuoteService.deleteQuote(1L))
          .isInstanceOf(QuoteNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("cancelHidden")
  class CancelHidden {

    @Test
    @DisplayName("숨김 해제 성공")
    void success() {
      // given
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.HIDDEN);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when
      Quote result = adminQuoteService.cancelHidden(1L);

      // then
      assertThat(result.getStatus()).isEqualTo(QuoteStatus.ACTIVE);
    }

    @Test
    @DisplayName("HIDDEN 상태가 아닌 문장 - 예외 발생")
    void notHidden() {
      // given
      Quote quote = createQuote(QuoteType.PUBLIC, QuoteStatus.ACTIVE);

      when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

      // when & then
      assertThatThrownBy(() -> adminQuoteService.cancelHidden(1L))
          .isInstanceOf(QuoteNotProcessableException.class);
    }
  }
}
