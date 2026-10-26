package com.wonnabe.classifier.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.wonnabe.classifier.domain.ClassificationMethod;
import com.wonnabe.classifier.domain.Category;
import com.wonnabe.classifier.domain.Transaction;

public record TransactionResponse(
		Long id,
		String merchantName,
		String merchantStoreType,
		BigDecimal amount,
		LocalDate transactionDate,
		Category category,
		ClassificationMethod classificationMethod
) {
	public static TransactionResponse from(Transaction transaction) {
		return new TransactionResponse(
				transaction.getId(),
				transaction.getMerchantName(),
				transaction.getMerchantStoreType(),
				transaction.getAmount(),
				transaction.getTransactionDate(),
				transaction.getCategory(),
				transaction.getClassificationMethod()
		);
	}
}
