package com.typingpractice.typing_practice_be.member.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {
  private final EntityManager em;

  @Override
  public Optional<Member> findById(Long memberId) {
    return em.createQuery("select m from Member m where m.id = :memberId", Member.class)
        .setParameter("memberId", memberId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  public Optional<Member> findByEmail(String email) {
    return em.createQuery("select m from Member m where m.email = :email", Member.class)
        .setParameter("email", email)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  public Long save(Member member) {
    em.persist(member);

    return member.getId();
  }

  public void deleteMember(Member member) {

    em.remove(member);
  }
}
