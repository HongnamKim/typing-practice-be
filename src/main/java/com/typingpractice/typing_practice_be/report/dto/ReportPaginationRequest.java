package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.dto.PaginationRequest;
import com.typingpractice.typing_practice_be.report.domain.ReportOrderBy;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ReportPaginationRequest extends PaginationRequest {

  private Long memberId;
  private ReportStatus status;
  private ReportOrderBy orderBy = ReportOrderBy.id;

  protected ReportPaginationRequest(int page, int size, SortDirection sortDirection) {
    super(page, size, sortDirection);
  }

  public static ReportPaginationRequest create(
      int page,
      int size,
      SortDirection sortDirection,
      ReportStatus status,
      ReportOrderBy orderBy,
      Long memberId) {
    ReportPaginationRequest request = new ReportPaginationRequest(page, size, sortDirection);

    request.status = status;
    request.memberId = memberId;
    request.orderBy = orderBy;

    return request;
  }
}
