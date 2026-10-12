package com.typingpractice.typing_practice_be.member.dto.admin;

import com.typingpractice.typing_practice_be.member.domain.Member;
import java.time.LocalDateTime;

/** 어드민 회원 목록용 프로젝션: 회원 + 문장/단어 통계를 합친 최근 타이핑 시각 (기록 없으면 null) */
public record MemberWithLastPracticedAt(Member member, LocalDateTime lastPracticedAt) {}
