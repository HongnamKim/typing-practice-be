package com.typingpractice.typing_practice_be.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
public class MemberListResponse {
  private List<MemberResponseDto> content;
  private int page;
  private int size;
  private boolean hasNext;
}
