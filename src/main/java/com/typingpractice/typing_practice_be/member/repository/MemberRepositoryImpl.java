package com.typingpractice.typing_practice_be.member.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.member.domain.MemberOrderBy;
import com.typingpractice.typing_practice_be.member.dto.admin.MemberWithLastPracticedAt;
import com.typingpractice.typing_practice_be.member.query.MemberPaginationQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {
  private final EntityManager em;

  @Override
  public boolean existByNickname(String nickname) {
    return em.createQuery("select m from Member m where m.nickname = :nickname", Member.class)
        .setParameter("nickname", nickname)
        .setMaxResults(1)
        .getResultStream()
        .findAny()
        .isPresent();
  }

  @Override
  public List<MemberWithLastPracticedAt> findAll(MemberPaginationQuery query) {
    int page = query.getPage();
    int size = query.getSize();

    // 문장(MemberTypingStats)·단어(MemberWordTypingStats) 통계의 언어별 lastPracticedAt 중 최댓값.
    // PostgreSQL greatest 는 null 을 무시하므로 둘 다 없을 때만 null.
    String jpql =
        "select m as member, greatest("
            + "(select max(s.lastPracticedAt) from MemberTypingStats s where s.member = m), "
            + "(select max(w.lastPracticedAt) from MemberWordTypingStats w where w.member = m)"
            + ") as lastPracticedAt from Member m";

    if (query.getRole() != null) {
      jpql += " where m.role = :role";
    }

    if (query.getOrderBy() == MemberOrderBy.lastPracticedAt) {
      jpql += " order by lastPracticedAt " + query.getSortDirection() + " nulls last, m.id desc";
    } else {
      jpql += " order by m." + query.getOrderBy() + " " + query.getSortDirection();
    }

    TypedQuery<Tuple> typedQuery = em.createQuery(jpql, Tuple.class);

    if (query.getRole() != null) {
      typedQuery.setParameter("role", query.getRole());
    }

    List<Tuple> rows =
        typedQuery.setFirstResult((page - 1) * size).setMaxResults(size + 1).getResultList();

    return rows.stream()
        .map(
            t ->
                new MemberWithLastPracticedAt(
                    t.get("member", Member.class), t.get("lastPracticedAt", LocalDateTime.class)))
        .toList();
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
  public List<Member> findAllByIds(List<Long> memberIds) {
    if (memberIds.isEmpty()) return List.of();

    return em.createQuery("select m from Member m where m.id in :ids", Member.class)
        .setParameter("ids", memberIds)
        .getResultList();
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
