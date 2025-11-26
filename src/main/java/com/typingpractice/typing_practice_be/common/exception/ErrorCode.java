package com.typingpractice.typing_practice_be.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
  DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다."),

  FORBIDDEN(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");

  private final HttpStatus status;
  private final String message;
}
