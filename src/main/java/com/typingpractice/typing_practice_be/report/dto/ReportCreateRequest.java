package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.report.domain.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Getter
@ToString
public class ReportCreateRequest {
  @NotNull(message = "신고 대상 오류")
  @Positive(message = "신고 대상 오류")
  private Long quoteId;

  @NotNull(message = "신고 사유 오류")
  private ReportReason reason;

  @Length(min = 1, max = 200)
  private String detail;

  public static ReportCreateRequest create(Long quoteId, ReportReason reason, String detail) {
    ReportCreateRequest request = new ReportCreateRequest();
    request.quoteId = quoteId;
    request.reason = reason;
    request.detail = detail;

    return request;
  }
}
