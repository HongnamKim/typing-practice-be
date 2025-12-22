package com.typingpractice.typing_practice_be.member.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberPaginationRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {
  private final EntityManager em;

  @Override
  public List<Member> findAll(MemberPaginationRequest request) {
    int page = request.getPage();
    int size = request.getSize();

    String jpql = "select m from Member m";

    if (request.getRole() != null) {
      jpql += " where m.role = :role";
    }
    jpql += (" order by m." + request.getOrderBy() + " " + request.getSortDirection());

    TypedQuery<Member> query = em.createQuery(jpql, Member.class);
    if (request.getRole() != null) {
      query.setParameter("role", request.getRole());
    }

    return query.setFirstResult((page - 1) * size).setMaxResults(size + 1).getResultList();
  }

  @Override
  public Optional<Member> findById(Long memberId) {
    return em.createQuery("select m from Member m where m.id = :memberId", Member.class)
        .setParameter("memberId", memberId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  @Override
  public Optional<Member> findByEmail(String email) {
    return em.createQuery("select m from Member m where m.email = :email", Member.class)
        .setParameter("email", email)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  @Override
  public Optional<Member> login(String email, String password) {
    return em.createQuery(
            "select m from Member m where m.email = :email and m.password = :password",
            Member.class)
        .setParameter("email", email)
        .setParameter("password", password)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  @Override
  public Long save(Member member) {
    em.persist(member);

    return member.getId();
  }

  @Override
  public void deleteMember(Member member) {

    em.remove(member);
  }
}
