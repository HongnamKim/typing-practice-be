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
public class JwtBlackList extends BaseEntity {
  @Id
  @GeneratedValue
  @Column(name = "token_blacklist_id")
  private Long id;

  private String jwtId;

  private LocalDateTime expiresIn;

  public static JwtBlackList create(String jwtId, LocalDateTime expiresIn) {
    JwtBlackList jwtBlackList = new JwtBlackList();
    jwtBlackList.jwtId = jwtId;
    jwtBlackList.expiresIn = expiresIn;

    return jwtBlackList;
  }
}
