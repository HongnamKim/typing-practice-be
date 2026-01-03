package com.typingpractice.typing_practice_be.report.query;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.query.PaginationQuery;
import com.typingpractice.typing_practice_be.report.domain.ReportOrderBy;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.dto.ReportPaginationRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ReportPaginationQuery extends PaginationQuery {
  private final Long memberId;
  private final ReportStatus status;
  private final ReportOrderBy orderBy;

  private ReportPaginationQuery(
      int page,
      int size,
      SortDirection sortDirection,
      Long memberId,
      ReportStatus status,
      ReportOrderBy orderBy) {
    super(page, size, sortDirection);

    this.memberId = memberId;
    this.status = status;
    this.orderBy = orderBy;
  }

  public static ReportPaginationQuery from(ReportPaginationRequest request) {
    return new ReportPaginationQuery(
        request.getPage(),
        request.getSize(),
        request.getSortDirection(),
        request.getMemberId(),
        request.getStatus(),
        request.getOrderBy());
  }
}
