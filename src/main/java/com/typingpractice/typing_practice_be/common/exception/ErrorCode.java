package com.typingpractice.typing_practice_be.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
  DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다."),

  NOT_ADMIN(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),
  QUOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예문을 찾을 수 없습니다."),
  QUOTE_NOT_OWNED(HttpStatus.FORBIDDEN, "문장에 대한 권한이 없습니다."),
  QUOTE_NOT_PROCESSABLE(HttpStatus.BAD_REQUEST, "처리할 수 없는 문장입니다."),

  EMPTY_UPDATE_REQUEST(HttpStatus.BAD_REQUEST, "수정할 내용이 없습니다."),

  REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."),
  DUPLICATE_REPORT(HttpStatus.BAD_REQUEST, "중복된 신고 내역이 존재합니다."),
  QUOTE_NOT_REPORTABLE(HttpStatus.BAD_REQUEST, "신고 불가능한 문장입니다."),
  REPORT_NOT_PROCESSABLE(HttpStatus.BAD_REQUEST, "처리할 수 없는 신고 내역입니다.");

  private final HttpStatus status;
  private final String message;
}
