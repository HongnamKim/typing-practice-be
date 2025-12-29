package com.typingpractice.typing_practice_be.report.dto;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.dto.PaginationRequest;
import com.typingpractice.typing_practice_be.report.domain.ReportOrderBy;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class ReportPaginationRequest extends PaginationRequest {

  private final Long memberId;
  private final ReportStatus status;
  private final ReportOrderBy orderBy;

  public ReportPaginationRequest(
      Integer page,
      Integer size,
      SortDirection sortDirection,
      ReportStatus status,
      ReportOrderBy orderBy,
      Long memberId) {
    super(page, size, sortDirection);
    this.status = status;
    this.orderBy = orderBy != null ? orderBy : ReportOrderBy.id;
    this.memberId = memberId;
  }

  /*protected ReportPaginationRequest(int page, int size, SortDirection sortDirection) {
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
    request.orderBy = orderBy != null ? orderBy : ReportOrderBy.id;

    return request;
  }*/
}
