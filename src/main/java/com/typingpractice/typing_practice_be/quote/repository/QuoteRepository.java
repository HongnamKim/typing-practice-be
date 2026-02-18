package com.typingpractice.typing_practice_be.quote.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import com.typingpractice.typing_practice_be.quote.domain.QuoteStatus;
import com.typingpractice.typing_practice_be.quote.domain.QuoteType;
import com.typingpractice.typing_practice_be.quote.query.PublicQuoteQuery;
import com.typingpractice.typing_practice_be.quote.query.QuotePaginationQuery;
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

  public List<Quote> findAll(QuotePaginationQuery query) {
    int page = query.getPage();
    int size = query.getSize();

    String jpql = "select q from Quote q";

    if (query.getStatus() != null && query.getType() != null) {
      jpql += " where q.status = :status and q.type = :type";
    } else if (query.getStatus() != null) {
      jpql += " where q.status = :status";
    } else if (query.getType() != null) {
      jpql += " where q.type = :type";
    }

    jpql += " order by q." + query.getOrderBy() + " " + query.getSortDirection();

    TypedQuery<Quote> typedQuery =
        em.createQuery(jpql, Quote.class).setFirstResult((page - 1) * size).setMaxResults(size + 1);

    if (query.getStatus() != null) {
      typedQuery.setParameter("status", query.getStatus());
    }

    if (query.getType() != null) {
      typedQuery.setParameter("type", query.getType());
    }

    return typedQuery.getResultList();
  }

  public List<Quote> findPublicQuotes(PublicQuoteQuery query) {
    // 랜덤 순서
    em.createNativeQuery("SELECT SETSEED(:seed)")
        .setParameter("seed", query.getSeed())
        .getSingleResult();

    // 내 문장만 조회
    if (query.getOnlyMyQuotes() && query.getMemberId() != null) {
      String jpql = "select q from Quote q where q.member.id = :memberId and q.status !=:status";

      // 랜덤 순서
      jpql += " order by function('RANDOM')";

      TypedQuery<Quote> typedQuery =
          em.createQuery(jpql, Quote.class)
              .setParameter("memberId", query.getMemberId())
              .setParameter("status", QuoteStatus.HIDDEN)
              .setFirstResult((query.getPage() - 1) * query.getCount())
              .setMaxResults(query.getCount() + 1);
      return typedQuery.getResultList();
    } else {
      // 전체 문장 조회
      String jpql = "select q from Quote q where (q.status = :status and q.type = :type)";

      if (query.getMemberId() != null) {
        jpql += " or (q.member.id = :memberId and q.status != :hiddenStatus)";
        // jpql += " or (q.member.id = :memberId and q.status = :myQuoteStatus and q.type =
        // :myQuoteType)";
      }

      // 랜덤 순서
      jpql += " order by function('RANDOM')";

      TypedQuery<Quote> typedQuery =
          em.createQuery(jpql, Quote.class)
              .setParameter("status", QuoteStatus.ACTIVE)
              .setParameter("type", QuoteType.PUBLIC)
              .setFirstResult((query.getPage() - 1) * query.getCount())
              .setMaxResults(query.getCount() + 1);

      if (query.getMemberId() != null) {
        typedQuery
            .setParameter("memberId", query.getMemberId())
            .setParameter("hiddenStatus", QuoteStatus.HIDDEN);
        // .setParameter("myQuoteType", QuoteType.PRIVATE);
      }

      return typedQuery.getResultList();
    }
  }

  /*public List<Quote> findByStatus(QuoteStatus quoteStatus) {
    return em.createQuery("select q from Quote q where q.status = :status", Quote.class)
        .setParameter("status", quoteStatus)
        .getResultList();
  }*/

  public void deleteQuote(Quote quote) {
    em.remove(quote);
  }

  public List<Quote> findByMember(Member member, QuotePaginationQuery query) {
    int page = query.getPage();
    int size = query.getSize();

    String jpql = "select q from Quote q join q.member m where m.id = :memberId";

    if (query.getStatus() != null && query.getType() != null) {
      jpql += " and q.status = :status and q.type = :type";
    } else if (query.getStatus() != null) {
      jpql += " and q.status = :status";
    } else if (query.getType() != null) {
      jpql += " and q.type = :type";
    }

    jpql += " order by q." + query.getOrderBy() + " " + query.getSortDirection();

    TypedQuery<Quote> typedQuery =
        em.createQuery(jpql, Quote.class).setFirstResult((page - 1) * size).setMaxResults(size + 1);
    typedQuery.setParameter("memberId", member.getId());

    if (query.getStatus() != null) {
      typedQuery.setParameter("status", query.getStatus());
    }

    if (query.getType() != null) {
      typedQuery.setParameter("type", query.getType());
    }

    return typedQuery.getResultList();
  }

  public Long findMaxIdByLanguage(QuoteLanguage language) {
    return em.createQuery("select max(q.id) from Quote q where q.language = :language", Long.class)
        .setParameter("language", language)
        .getSingleResult();
  }

  public List<Quote> findPageByLanguageAndIdRange(
      QuoteLanguage language, Long cursorId, Long maxId, int size) {
    return em.createQuery(
            "select q from Quote q where q.language = :language and q.id > :cursorId and q.id <= :maxId order by q.id ASC",
            Quote.class)
        .setParameter("language", language)
        .setParameter("cursorId", cursorId)
        .setParameter("maxId", maxId)
        .setMaxResults(size)
        .getResultList();
  }
}
