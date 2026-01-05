package com.typingpractice.typing_practice_be.member.query;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.query.PaginationQuery;
import com.typingpractice.typing_practice_be.member.domain.MemberOrderBy;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberPaginationRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class MemberPaginationQuery extends PaginationQuery {
  private final MemberRole role;
  private final MemberOrderBy orderBy;

  private MemberPaginationQuery(
      int page, int size, SortDirection sortDirection, MemberRole role, MemberOrderBy orderBy) {
    super(page, size, sortDirection);

    this.role = role;
    this.orderBy = orderBy;
  }

  public static MemberPaginationQuery from(MemberPaginationRequest request) {
    return new MemberPaginationQuery(
        request.getPage(),
        request.getSize(),
        request.getSortDirection(),
        request.getRole(),
        request.getOrderBy());
  }
}
