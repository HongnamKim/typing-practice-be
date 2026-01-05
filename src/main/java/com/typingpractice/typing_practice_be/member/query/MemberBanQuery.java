package com.typingpractice.typing_practice_be.member.query;

import com.typingpractice.typing_practice_be.member.dto.admin.MemberBanRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberBanQuery {
  private final String banReason;

  private MemberBanQuery(String banReason) {
    this.banReason = banReason;
  }

  public static MemberBanQuery from(MemberBanRequest request) {
    return new MemberBanQuery(request.getBanReason());
  }
}
