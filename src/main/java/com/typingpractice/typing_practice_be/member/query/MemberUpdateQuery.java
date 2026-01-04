package com.typingpractice.typing_practice_be.member.query;

import com.typingpractice.typing_practice_be.member.dto.UpdateNicknameRequest;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberUpdateQuery {
  private final String nickname;

  private MemberUpdateQuery(String nickname) {
    this.nickname = nickname;
  }

  public static MemberUpdateQuery from(UpdateNicknameRequest request) {
    return new MemberUpdateQuery(request.getNickname());
  }
}
