package com.typingpractice.typing_practice_be.quote.dto;

import com.typingpractice.typing_practice_be.quote.domain.QuoteLanguage;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.Range;

@Getter
@ToString
public class PublicQuoteRequest {
	@Min(value = 1)
	private final Integer page;

	@Range(min = 10, max = 300)
	private final Integer count;

	private final Boolean onlyMyQuotes;

	private final QuoteLanguage language;

	@NotNull
	@DecimalMin("-999999999")
	@DecimalMax("999999999")
	private final Float seed;

	public PublicQuoteRequest(
					Integer page, Integer count, Boolean onlyMyQuotes, Float seed, QuoteLanguage language) {
		this.page = page != null ? page : 1;
		this.count = count != null ? count : 100;
		this.onlyMyQuotes = onlyMyQuotes != null ? onlyMyQuotes : false;
		this.seed = seed;
		this.language = language;
		// this.seed = seed != null ? seed : (float) (Math.random() * 2 - 1);
	}
}
