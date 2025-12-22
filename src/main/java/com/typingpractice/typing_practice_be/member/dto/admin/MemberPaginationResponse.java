package com.typingpractice.typing_practice_be.member.dto.admin;

import com.typingpractice.typing_practice_be.common.dto.PaginationResponse;
import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.MemberResponse;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class MemberPaginationResponse extends PaginationResponse {
  private List<MemberResponse> content;

  protected MemberPaginationResponse(int page, int size, boolean hasNext) {
    super(page, size, hasNext);
  }

  public static MemberPaginationResponse from(
      List<Member> members, int page, int size, boolean hasNext) {
    MemberPaginationResponse response = new MemberPaginationResponse(page, size, hasNext);

    response.content = members.stream().limit(size).map(MemberResponse::from).toList();

    return response;
  }
}
