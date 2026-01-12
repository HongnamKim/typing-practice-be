package com.typingpractice.typing_practice_be.auth.repository;

import com.typingpractice.typing_practice_be.auth.domain.JwtBlackList;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class JwtBlackListRepository {
  private final EntityManager em;

  public Long save(JwtBlackList jwtBlackList) {
    em.persist(jwtBlackList);

    return jwtBlackList.getId();
  }

  public boolean existByJwtId(String jwtId) {

    return !em.createQuery("select j from JwtBlackList j where jwtId = :jwtId", JwtBlackList.class)
        .setParameter("jwtId", jwtId)
        .getResultList()
        .isEmpty();
  }

  public void deleteExpiredTokens(LocalDateTime now) {
    em.createQuery("delete from JwtBlackList j where j.expiresIn < :now")
        .setParameter("now", now)
        .executeUpdate();
  }
}
