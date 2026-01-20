package com.typingpractice.typing_practice_be.common.security.admin;

import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import com.typingpractice.typing_practice_be.member.exception.NotAdminException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AdminCheckAspect {
  @Before(
      "@within(com.typingpractice.typing_practice_be.common.security.admin.AdminOnly) || @annotation(com.typingpractice.typing_practice_be.common.security.admin.AdminOnly)")
  public void checkAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean isAdmin =
        auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(MemberRole.ADMIN.getAuthority()));

    if (!isAdmin) {
      throw new NotAdminException();
    }
  }
}
