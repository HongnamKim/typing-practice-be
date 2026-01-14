package com.typingpractice.typing_practice_be.common.jwt;

import com.typingpractice.typing_practice_be.auth.JwtProperties;
import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
  private final SecretKey secretKey;
  private final long accessTokenExpiration;

  public JwtTokenProvider(JwtProperties jwtProperties) {
    this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiration = jwtProperties.getAccessTokenExpirationMs();
  }

  public String createToken(Long memberId, MemberRole role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + accessTokenExpiration); // 만료 시간

    return Jwts.builder()
        .subject(String.valueOf(memberId))
        .claim("role", role.name())
        .claim("jwtId", UUID.randomUUID().toString())
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(secretKey)
        .compact();
  }

  public Long getMemberId(String token) {
    Claims claims = parseClaims(token);
    return Long.valueOf(claims.getSubject());
  }

  public String getRole(String token) {
    Claims claims = parseClaims(token);
    return claims.get("role", String.class);
  }

  public JwtPayload getJwtPayload(String token) {
    Claims claims = parseClaims(token);
    return JwtPayload.create(claims);
  }

  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }
}
