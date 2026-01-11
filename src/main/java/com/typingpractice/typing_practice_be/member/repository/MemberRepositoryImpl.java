package com.typingpractice.typing_practice_be.member.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.query.MemberPaginationQuery;
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
  public List<Member> findAll(MemberPaginationQuery query) {
    int page = query.getPage();
    int size = query.getSize();

    String jpql = "select m from Member m";

    if (query.getRole() != null) {
      jpql += " where m.role = :role";
    }

    jpql += (" order by m." + query.getOrderBy() + " " + query.getSortDirection());

    TypedQuery<Member> typedQuery = em.createQuery(jpql, Member.class);

    if (query.getRole() != null) {
      typedQuery.setParameter("role", query.getRole());
    }

    return typedQuery.setFirstResult((page - 1) * size).setMaxResults(size + 1).getResultList();
  }

  @Override
  public Optional<Member> findByProviderId(String providerId) {
    return em.createQuery("select m from Member m where m.providerId = :providerId", Member.class)
        .setParameter("providerId", providerId)
        .setMaxResults(1)
        .getResultStream()
        .findAny();
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
  public Long save(Member member) {
    em.persist(member);

    return member.getId();
  }

  @Override
  public void deleteMember(Member member) {

    em.remove(member);
  }
}
