package com.wonnabe.classifier.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String merchantName;

	private String merchantStoreType;

	private BigDecimal amount;

	private LocalDate transactionDate;

	@Enumerated(EnumType.STRING)
	private Category category;

	@Enumerated(EnumType.STRING)
	private ClassificationMethod classificationMethod;

	private LocalDateTime createdAt;

	@Builder
	public Transaction(String merchantName, String merchantStoreType, BigDecimal amount,
						LocalDate transactionDate, Category category, ClassificationMethod classificationMethod) {
		this.merchantName = merchantName;
		this.merchantStoreType = merchantStoreType;
		this.amount = amount;
		this.transactionDate = transactionDate;
		this.category = category;
		this.classificationMethod = classificationMethod;
		this.createdAt = LocalDateTime.now();
	}

	public void classify(Category category, ClassificationMethod classificationMethod) {
		this.category = category;
		this.classificationMethod = classificationMethod;
	}
}
