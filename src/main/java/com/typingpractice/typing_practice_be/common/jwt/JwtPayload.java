package com.typingpractice.typing_practice_be.common.jwt;

import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class JwtPayload {
  private Long memberId;

  private MemberRole role;

  private String jwtId;

  private LocalDateTime issuedAt;
  private LocalDateTime expiresIn;

  private JwtPayload() {}

  public static JwtPayload create(Long memberId, String jwtId, LocalDateTime expiresIn) {
    JwtPayload jwtPayload = new JwtPayload();
    jwtPayload.memberId = memberId;
    jwtPayload.jwtId = jwtId;
    jwtPayload.expiresIn = expiresIn;
    return jwtPayload;
  }

  public static JwtPayload create(Claims jwtClaims) {
    JwtPayload jwtPayload = new JwtPayload();

    jwtPayload.memberId = Long.valueOf(jwtClaims.getSubject());
    jwtPayload.role = MemberRole.valueOf(jwtClaims.get("role", String.class));

    jwtPayload.jwtId = jwtClaims.get("jwtId", String.class);

    jwtPayload.issuedAt =
        LocalDateTime.ofInstant(jwtClaims.getIssuedAt().toInstant(), ZoneOffset.UTC);
    jwtPayload.expiresIn =
        LocalDateTime.ofInstant(jwtClaims.getExpiration().toInstant(), ZoneOffset.UTC);

    return jwtPayload;
  }
}
