package com.typingpractice.typing_practice_be.common.jwt;

import com.typingpractice.typing_practice_be.member.domain.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
  private final SecretKey secretKey;
  private final long expiration;

  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiration = expiration;
  }

  public String createToken(Long memberId, MemberRole role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration); // 만료 시간

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
