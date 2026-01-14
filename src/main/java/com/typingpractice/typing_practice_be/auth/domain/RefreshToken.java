package com.typingpractice.typing_practice_be.auth.domain;

import com.typingpractice.typing_practice_be.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "refresh_token_id")
  private Long id;

  private Long memberId;

  private String token;

  private LocalDateTime expiresIn;

  public static RefreshToken create(Long memberId, String token, LocalDateTime expiresIn) {
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.memberId = memberId;
    refreshToken.token = token;
    refreshToken.expiresIn = expiresIn;

    return refreshToken;
  }
}
