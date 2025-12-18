package com.typingpractice.typing_practice_be.report.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.domain.Quote;
import com.typingpractice.typing_practice_be.report.domain.Report;
import com.typingpractice.typing_practice_be.report.domain.ReportStatus;
import com.typingpractice.typing_practice_be.report.dto.ReportRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepository {
  private final EntityManager em;

  public Report save(Report report) {
    em.persist(report);

    return report;
  }

  public List<Report> findAll(ReportRequest request) {
    //    return em.createQuery("select r from Report r where r.status = :status", Report.class)
    //        .setParameter("status", status)
    //        .getResultList();

    String jpql = "select r from Report r";

    if (request.getStatus() != null) {
      jpql += " where r.status = :status";
    }

    TypedQuery<Report> query = em.createQuery(jpql, Report.class);

    if (request.getStatus() != null) {
      query.setParameter("status", request.getStatus());
    }

    return query.getResultList();
  }

  public Optional<Report> findById(Long reportId) {
    return Optional.ofNullable(em.find(Report.class, reportId));
  }

  public List<Report> findMyReports(Member member) {
    return em.createQuery("select r from Report r where r.member.id = :memberId", Report.class)
        .setParameter("memberId", member.getId())
        .getResultList();
  }

  public List<Report> findByQuote(Quote quote) {
    return em.createQuery(
            "select r from Report r join r.quote q where q.id = :quoteId", Report.class)
        .setParameter("quoteId", quote.getId())
        .getResultList();
  }

  public void processReportByQuote(Quote quote) {
    // quote 에 해당하는 모든 신고내역 처리
    em.createQuery(
            "update Report r set r.status = :status, r.updatedAt = :updatedAt where r.quote.id = :quoteId")
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
