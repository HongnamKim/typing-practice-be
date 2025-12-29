package com.typingpractice.typing_practice_be.report.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.dto.ReportPaginationRequest;
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

  public List<Report> findAll(ReportPaginationRequest request) {
    int page = request.getPage();
    int size = request.getSize();

    String jpql = "select r from Report r join fetch r.member m left join fetch r.quote q";

    if (request.getStatus() != null && request.getMemberId() != null) {
      jpql += " where r.status = :status and m.id = :memberId";
    } else if (request.getStatus() == null && request.getMemberId() != null) {
      jpql += " where m.id = :memberId";
    } else if (request.getStatus() != null) {
      jpql += " where r.status = :status";
    }

    jpql += " order by r." + request.getOrderBy() + " " + request.getSortDirection();

    TypedQuery<Report> query =
        em.createQuery(jpql, Report.class)
            .setFirstResult((page - 1) * size)
            .setMaxResults(size + 1);

    if (request.getStatus() != null) {
      query.setParameter("status", request.getStatus());
    }

    if (request.getMemberId() != null) {
      query.setParameter("memberId", request.getMemberId());
    }

    return query.getResultList();
  }

  public Optional<Report> findById(Long reportId) {
    return Optional.ofNullable(em.find(Report.class, reportId));
  }

  public List<Report> findMyReports(Member member, ReportPaginationRequest request) {
    int page = request.getPage();
    int size = request.getSize();

    String jpql = "select r from Report r left join fetch r.quote where r.member.id = :memberId";

    if (request.getStatus() != null) {
      jpql += " and r.status = :status";
    }

    jpql += " order by r." + request.getOrderBy() + " " + request.getSortDirection();

    TypedQuery<Report> query = em.createQuery(jpql, Report.class);

    query.setFirstResult((page - 1) * size).setMaxResults(size + 1);

    query.setParameter("memberId", member.getId());

    if (request.getStatus() != null) {
      query.setParameter("status", request.getStatus());
    }

    return query.getResultList();
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
