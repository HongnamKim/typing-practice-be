package com.typingpractice.typing_practice_be.member.dto.admin;

import com.typingpractice.typing_practice_be.common.dto.PageResult;
import com.typingpractice.typing_practice_be.common.dto.PaginationResponse;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class MemberPaginationResponse extends PaginationResponse {
  private List<AdminMemberResponse> content;

  protected MemberPaginationResponse(int page, int size, boolean hasNext) {
    super(page, size, hasNext);
  }

  public static MemberPaginationResponse from(PageResult<MemberWithLastPracticedAt> result) {
    MemberPaginationResponse response =
        new MemberPaginationResponse(result.getPage(), result.getSize(), result.isHasNext());

    response.content = result.getContent().stream().map(AdminMemberResponse::from).toList();

    return response;
  }
}
