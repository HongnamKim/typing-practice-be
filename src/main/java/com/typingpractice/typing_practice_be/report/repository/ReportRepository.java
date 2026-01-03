package com.typingpractice.typing_practice_be.report.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.query.ReportPaginationQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReportRepository {
  private final EntityManager em;

  public Report save(Report report) {
    em.persist(report);

    return report;
  }

  public List<Report> findAll(ReportPaginationQuery query) {
    int page = query.getPage();
    int size = query.getSize();

    String jpql = "select r from Report r join fetch r.member m left join fetch r.quote q";

    if (query.getStatus() != null && query.getMemberId() != null) {
      jpql += " where r.status = :status and m.id = :memberId";
    } else if (query.getStatus() == null && query.getMemberId() != null) {
      jpql += " where m.id = :memberId";
    } else if (query.getStatus() != null) {
      jpql += " where r.status = :status";
    }

    jpql += " order by r." + query.getOrderBy() + " " + query.getSortDirection();

    TypedQuery<Report> typedQuery =
        em.createQuery(jpql, Report.class)
            .setFirstResult((page - 1) * size)
            .setMaxResults(size + 1);

    if (query.getStatus() != null) {
      typedQuery.setParameter("status", query.getStatus());
    }

    if (query.getMemberId() != null) {
      typedQuery.setParameter("memberId", query.getMemberId());
    }

    return typedQuery.getResultList();
  }

  public Optional<Report> findById(Long reportId) {
    return Optional.ofNullable(em.find(Report.class, reportId));
  }

  public List<Report> findMyReports(Member member, ReportPaginationQuery query) {
    int page = query.getPage();
    int size = query.getSize();

    String jpql = "select r from Report r left join fetch r.quote where r.member.id = :memberId";

    if (query.getStatus() != null) {
      jpql += " and r.status = :status";
    }

    jpql += " order by r." + query.getOrderBy() + " " + query.getSortDirection();

    TypedQuery<Report> typedQuery = em.createQuery(jpql, Report.class);

    typedQuery.setFirstResult((page - 1) * size).setMaxResults(size + 1);

    typedQuery.setParameter("memberId", member.getId());

    if (query.getStatus() != null) {
      typedQuery.setParameter("status", query.getStatus());
    }

    return typedQuery.getResultList();
  }

  public List<Report> findByQuote(Quote quote) {
    return em.createQuery(
            "select r from Report r join r.quote q where q.id = :quoteId", Report.class)
        .setParameter("quoteId", quote.getId())
        .getResultList();
  }

  public void processReportByQuote(Quote quote, boolean quoteDeleted) {
    // quote 에 해당하는 모든 신고내역 처리
    em.createQuery(
            "update Report r set r.status = :status, r.updatedAt = :updatedAt, r.quoteDeleted = :quoteDeleted where r.quote.id = :quoteId")
        .setParameter("quoteDeleted", quoteDeleted)
        .setParameter("status", ReportStatus.PROCESSED)
        .setParameter("updatedAt", LocalDateTime.now())
        .setParameter("quoteId", quote.getId())
        .executeUpdate();
  }

  public boolean existsByQuoteAndMember(Quote quote, Member member) {
    List<Report> resultList =
        em.createQuery(
                "select r from Report r join r.quote q join r.member m where q.id = :quoteId and m.id = :memberId",
                Report.class)
            .setParameter("quoteId", quote.getId())
            .setParameter("memberId", member.getId())
            .setMaxResults(1)
            .getResultList();

    return !resultList.isEmpty();
  }

  public void delete(Report report) {
    em.remove(report);
  }
}
