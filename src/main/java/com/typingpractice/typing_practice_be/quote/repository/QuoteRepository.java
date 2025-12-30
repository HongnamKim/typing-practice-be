package com.typingpractice.typing_practice_be.quote.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.dto.QuotePaginationRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QuoteRepository {
  private final EntityManager em;

  public void save(Quote quote) {
    em.persist(quote);
  }

  public Optional<Quote> findById(Long quoteId) {
    return em.createQuery(
            "select q from Quote q join fetch q.member m where q.id = :quoteId", Quote.class)
        .setParameter("quoteId", quoteId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();

    // return Optional.ofNullable(em.find(Quote.class, quoteId));
  }

  public List<Quote> findAll(QuotePaginationRequest request) {
    int page = request.getPage();
    int size = request.getSize();

    String jpql = "select q from Quote q";

    if (request.getStatus() != null && request.getType() != null) {
      jpql += " where q.status = :status and q.type = :type";
    } else if (request.getStatus() != null) {
      jpql += " where q.status = :status";
    } else if (request.getType() != null) {
      jpql += " where q.type = :type";
    }

    jpql += " order by q." + request.getOrderBy() + " " + request.getSortDirection();

    TypedQuery<Quote> query =
        em.createQuery(jpql, Quote.class).setFirstResult((page - 1) * size).setMaxResults(size + 1);

    if (request.getStatus() != null) {
      query.setParameter("status", request.getStatus());
    }

    if (request.getType() != null) {
      query.setParameter("type", request.getType());
    }

    return query.getResultList();
  }

  public List<Quote> findPublicQuotes(long memberId, int count, boolean onlyMyQuotes) {
    String jpql = "select q from Quote q where q.status = :status and q.type = :type";

    if (onlyMyQuotes) {
      jpql += " and q.member.id = :memberId";
    }

    jpql += " order by function('RANDOM')";

    TypedQuery<Quote> query =
        em.createQuery(jpql, Quote.class)
            .setParameter("status", QuoteStatus.ACTIVE)
            .setParameter("type", QuoteType.PUBLIC)
            .setMaxResults(count);

    if (onlyMyQuotes) {
      query.setParameter("memberId", memberId);
    }

    return query.getResultList();
  }

  public List<Quote> findByStatus(QuoteStatus quoteStatus) {
    return em.createQuery("select q from Quote q where q.status = :status", Quote.class)
        .setParameter("status", quoteStatus)
        .getResultList();
  }

  public void deleteQuote(Quote quote) {
    em.remove(quote);
  }

  public List<Quote> findByMember(Member member, QuotePaginationRequest request) {
    int page = request.getPage();
    int size = request.getSize();

    String jpql = "select q from Quote q join q.member m where m.id = :memberId";

    if (request.getStatus() != null && request.getType() != null) {
      jpql += " and q.status = :status and q.type = :type";
    } else if (request.getStatus() != null) {
      jpql += " and q.status = :status";
    } else if (request.getType() != null) {
      jpql += " and q.type = :type";
    }

    jpql += " order by q." + request.getOrderBy() + " " + request.getSortDirection();

    TypedQuery<Quote> query =
        em.createQuery(jpql, Quote.class).setFirstResult((page - 1) * size).setMaxResults(size + 1);
    query.setParameter("memberId", member.getId());

    if (request.getStatus() != null) {
      query.setParameter("status", request.getStatus());
    }

    if (request.getType() != null) {
      query.setParameter("type", request.getType());
    }

    return query.getResultList();
  }
}
