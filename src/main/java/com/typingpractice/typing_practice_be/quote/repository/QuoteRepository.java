package com.typingpractice.typing_practice_be.quote.repository;

import com.typingpractice.typing_practice_be.member.domain.Member;
import com.typingpractice.typing_practice_be.quote.config.SimilarityThresholdProperties;
import com.typingpractice.typing_practice_be.quote.domain.*;
import com.typingpractice.typing_practice_be.quote.query.PublicQuoteQuery;
import com.typingpractice.typing_practice_be.quote.query.QuotePaginationQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QuoteRepository {
	private final EntityManager em;

	private final SimilarityThresholdProperties thresholdProperties;

	public void save(Quote quote) {
		em.persist(quote);
	}

	public Optional<Quote> findById(Long quoteId) {
		return em.createQuery(
										"select q from Quote q left join fetch q.member m where q.id = :quoteId", Quote.class)
						.setParameter("quoteId", quoteId)
						.setMaxResults(1)
						.getResultStream()
						.findFirst();
	}

	public Optional<Quote> findByIdWithTypingStats(Long quoteId) {
		return em.createQuery(
										"select q from Quote q left join fetch q.typingStats where q.id = :quoteId",
										Quote.class)
						.setParameter("quoteId", quoteId)
						.getResultStream()
						.findFirst();
	}

	public List<Quote> findByIds(List<Long> ids, QuoteLanguage language) {
		if (ids.isEmpty()) return List.of();

		return em.createQuery(
										"select q from Quote q where q.id in :ids and q.language = :language", Quote.class)
						.setParameter("ids", ids)
						.setParameter("language", language)
						.getResultList();
	}

	public List<Long> findIdsByMemberId(Long memberId, QuoteLanguage language) {
		return em.createQuery(
										"select q.id from Quote q where q.member.id = :memberId and q.status != :status and q.language = :language",
										Long.class)
						.setParameter("memberId", memberId)
						.setParameter("status", QuoteStatus.HIDDEN)
						.setParameter("language", language)
						.setHint("org.hibernate.fetchSize", 100)
						.getResultList();
	}

	public List<Long> findAllPublicIds(QuoteLanguage language, QuoteDifficultyTier tier) {
		String jpql =
						"select q.id from Quote q where q.type = :type and q.status = :status and q.language = :language";

		if (tier != QuoteDifficultyTier.ALL) {
			jpql += " and q.difficulty >= :min and q.difficulty <= :max";
		}

		TypedQuery<Long> query =
						em.createQuery(jpql, Long.class)
										.setParameter("type", QuoteType.PUBLIC)
										.setParameter("status", QuoteStatus.ACTIVE)
										.setParameter("language", language);

		if (tier != QuoteDifficultyTier.ALL) {
			query.setParameter("min", tier.getMin()).setParameter("max", tier.getMax());
		}

		return query.getResultList();
	}

	public List<Quote> findAll(QuotePaginationQuery query) {
		int page = query.getPage();
		int size = query.getSize();

		String jpql = "select q from Quote q left join fetch q.typingStats";

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
			String jpql =
							"select q from Quote q where q.member.id = :memberId and q.status !=:status and q.language = :language";

			// 랜덤 순서
			jpql += " order by function('RANDOM')";

			TypedQuery<Quote> typedQuery =
							em.createQuery(jpql, Quote.class)
											.setParameter("memberId", query.getMemberId())
											.setParameter("status", QuoteStatus.HIDDEN)
											.setParameter("language", query.getLanguage())
											.setFirstResult((query.getPage() - 1) * query.getCount())
											.setMaxResults(query.getCount() + 1);
			return typedQuery.getResultList();
		} else {
			// 전체 문장 조회
			String jpql =
							"select q from Quote q where (q.status = :status and q.type = :type and q.language = :language)";

			if (query.getMemberId() != null) {
				jpql +=
								" or (q.member.id = :memberId and q.status != :hiddenStatus and q.language = :language)";
				// jpql += " or (q.member.id = :memberId and q.status = :myQuoteStatus and q.type =
				// :myQuoteType)";
			}

			// 랜덤 순서
			jpql += " order by function('RANDOM')";

			TypedQuery<Quote> typedQuery =
							em.createQuery(jpql, Quote.class)
											.setParameter("status", QuoteStatus.ACTIVE)
											.setParameter("type", QuoteType.PUBLIC)
											.setParameter("language", query.getLanguage())
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

	public boolean existsBySentenceHash(String sentenceHash, Long memberId) {
		Long count =
						em.createQuery(
														"select count(q) from Quote q where q.sentenceHash = :hash and (q.member.id = :memberId or q.type = :publicType)",
														Long.class)
										.setParameter("hash", sentenceHash)
										.setParameter("memberId", memberId)
										.setParameter("publicType", QuoteType.PUBLIC)
										.getSingleResult();

		return count > 0;
	}

	public boolean existsBySentenceHashExcluding(String sentenceHash, Long memberId, Long excludeId) {
		Long count =
						em.createQuery(
														"select count(q) from Quote q where q.id != :excludeId and q.sentenceHash = :hash and (q.member.id = :memberId or q.type = :publicType)",
														Long.class)
										.setParameter("hash", sentenceHash)
										.setParameter("memberId", memberId)
										.setParameter("publicType", QuoteType.PUBLIC)
										.setParameter("excludeId", excludeId)
										.getSingleResult();

		return count > 0;
	}

	public boolean existsBySentenceHashInMyQuotes(String sentenceHash, Long memberId) {
		Long count =
						em.createQuery(
														"select count(q) from Quote q where q.sentenceHash = :hash and q.member.id = :memberId",
														Long.class)
										.setParameter("hash", sentenceHash)
										.setParameter("memberId", memberId)
										.getSingleResult();

		return count > 0;
	}

	public boolean existsBySentenceHashInMyQuotesExcluding(
					String sentenceHash, Long memberId, Long excludeId) {
		Long count =
						em.createQuery(
														"select count(q) from Quote q where q.sentenceHash = :hash and q.member.id = :memberId and q.id != :excludeId",
														Long.class)
										.setParameter("hash", sentenceHash)
										.setParameter("memberId", memberId)
										.setParameter("excludeId", excludeId)
										.getSingleResult();

		return count > 0;
	}

	// 공개 업로드: 내 문장 + 공개 문장
	@SuppressWarnings("unchecked")
	public Optional<Object[]> findMostSimilar(
					String sentence, QuoteLanguage language, Long memberId) {
		List<Object[]> results =
						em.createNativeQuery(
														"SELECT sentence, similarity(sentence, :sentence) AS sim FROM quote "
																		+ "WHERE deleted = false AND language = :language "
																		+ "AND (member_id = :memberId or type = 'PUBLIC') "
																		+ "AND similarity(sentence, :sentence) > :threshold "
																		+ "ORDER BY sim DESC LIMIT 1")
										.setParameter("sentence", sentence)
										.setParameter("language", language.name())
										.setParameter("memberId", memberId)
										.setParameter("threshold", thresholdProperties.getByLanguage(language))
										.getResultList();

		return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
	}

	// 비공개 업로드: 내 문장만
	@SuppressWarnings("unchecked")
	public Optional<Object[]> findMostSimilarInMyQuotes(
					String sentence, QuoteLanguage language, Long memberId) {
		List<Object[]> results =
						em.createNativeQuery(
														"select sentence, similarity(sentence, :sentence) as sim from quote "
																		+ "where deleted = false and language = :language "
																		+ "and member_id = :memberId "
																		+ "and similarity(sentence, :sentence) > :threshold "
																		+ "order by sim desc limit 1")
										.setParameter("sentence", sentence)
										.setParameter("language", language.name())
										.setParameter("memberId", memberId)
										.setParameter("threshold", thresholdProperties.getByLanguage(language))
										.getResultList();

		return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
	}

	// 공개 전환: 내 문장 + 공개 문장, 자기 제외
	@SuppressWarnings("unchecked")
	public Optional<Object[]> findMostSimilarExcluding(
					String sentence, QuoteLanguage language, Long memberId, Long excludeId) {
		List<Object[]> results =
						em.createNativeQuery(
														"select sentence, similarity(sentence, :sentence) as sim from quote "
																		+ "where deleted = false and language = :language "
																		+ "and quote_id != :excludeId "
																		+ "and (member_id = :memberId OR type = 'PUBLIC') "
																		+ "and similarity(sentence, :sentence) > :threshold "
																		+ "order by sim desc limit 1")
										.setParameter("sentence", sentence)
										.setParameter("language", language.name())
										.setParameter("memberId", memberId)
										.setParameter("excludeId", excludeId)
										.setParameter("threshold", thresholdProperties.getByLanguage(language))
										.getResultList();

		return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
	}

	// 비공개 수정: 내 문장만, 자기 제외
	@SuppressWarnings("unchecked")
	public Optional<Object[]> findMostSimilarInMyQuotesExcluding(
					String sentence, QuoteLanguage language, Long memberId, Long excludeId) {
		List<Object[]> results =
						em.createNativeQuery(
														"select sentence, similarity(sentence, :sentence) as sim from quote "
																		+ "where deleted = false and language = :language "
																		+ "and member_id = :memberId and quote_id != :excludeId "
																		+ "and similarity(sentence, :sentence) > :threshold "
																		+ "order by sim desc limit 1")
										.setParameter("sentence", sentence)
										.setParameter("language", language.name())
										.setParameter("memberId", memberId)
										.setParameter("excludeId", excludeId)
										.setParameter("threshold", thresholdProperties.getByLanguage(language))
										.getResultList();

		return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
	}

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

	public long countByPeriod(QuoteLanguage language, LocalDateTime from, LocalDateTime to) {
		return em.createQuery(
										"select count(q) from Quote q "
														+ "where q.createdAt >= :from and q.createdAt <= :to "
														+ "and q.language = :language",
										Long.class)
						.setParameter("from", from)
						.setParameter("to", to)
						.setParameter("language", language)
						.getSingleResult();
	}
}
