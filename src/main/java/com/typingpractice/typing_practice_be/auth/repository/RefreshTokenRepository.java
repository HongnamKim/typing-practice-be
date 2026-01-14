package com.typingpractice.typing_practice_be.auth.repository;

import com.typingpractice.typing_practice_be.auth.domain.RefreshToken;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
  private final EntityManager em;

  public Long save(RefreshToken refreshToken) {
    em.persist(refreshToken);

    return refreshToken.getId();
  }

  public Optional<RefreshToken> findByToken(String token) {
    return em.createQuery("select t from RefreshToken t where t.token = :token", RefreshToken.class)
        .setParameter("token", token)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  public void deleteByToken(String token) {
    em.createQuery("delete from RefreshToken t where t.token = :token")
        .setParameter("token", token)
        .executeUpdate();
  }

  public void deleteByMemberId(Long memberId) {
    em.createQuery("delete from RefreshToken t where t.memberId = :memberId")
        .setParameter("memberId", memberId)
        .executeUpdate();
  }

  public void deleteExpiredTokens(LocalDateTime now) {
    em.createQuery("delete from RefreshToken t where t.expiresIn < :now")
        .setParameter("now", now)
        .executeUpdate();
  }
}
