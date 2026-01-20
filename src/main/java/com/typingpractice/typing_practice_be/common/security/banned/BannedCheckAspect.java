package com.typingpractice.typing_practice_be.common.security.banned;

import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.exception.BannedMemberException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BannedCheckAspect {
  @Before("@annotation(BannedNotAllowed)")
  public void checkBanned() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean isBanned =
        auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(MemberRole.BANNED.getAuthority()));

    if (isBanned) {
      throw new BannedMemberException();
    }
  }
}
