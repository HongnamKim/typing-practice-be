package com.typingpractice.typing_practice_be.member.dto.admin;

import com.typingpractice.typing_practice_be.common.domain.SortDirection;
import com.typingpractice.typing_practice_be.common.dto.PaginationRequest;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.domain.MemberOrderBy;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class MemberPaginationRequest extends PaginationRequest {

  private MemberRole role;

  private MemberOrderBy orderBy = MemberOrderBy.id;

  protected MemberPaginationRequest(int page, int size, SortDirection sortDirection) {
    super(page, size, sortDirection);
  }

  public static MemberPaginationRequest create(
      Integer page,
      Integer size,
      MemberRole role,
      MemberOrderBy orderBy,
      SortDirection sortDirection) {
    MemberPaginationRequest request = new MemberPaginationRequest(page, size, sortDirection);

    request.role = role;
    request.orderBy = orderBy;

    return request;
  }
}
