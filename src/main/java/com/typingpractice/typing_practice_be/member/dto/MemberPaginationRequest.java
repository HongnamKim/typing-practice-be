package com.typingpractice.typing_practice_be.member.dto;

import com.typingpractice.typing_practice_be.common.SortDirection;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MemberPaginationRequest {
  @Positive(message = "1 이상")
  private int page = 1;

  @Positive(message = "1 이상")
  @Max(value = 100, message = "최대 100")
  private int size = 10;

  private MemberRole role;

  private MemberOrderBy orderBy = MemberOrderBy.id;
  private SortDirection sortDirection = SortDirection.ASC;

  public static MemberPaginationRequest create(
      Integer page,
      Integer size,
      MemberRole role,
      MemberOrderBy orderBy,
      SortDirection sortDirection) {
    MemberPaginationRequest request = new MemberPaginationRequest();
    request.page = page;
    request.size = size;
    request.role = role;
    request.orderBy = orderBy;
    request.sortDirection = sortDirection;

    return request;
  }
}
