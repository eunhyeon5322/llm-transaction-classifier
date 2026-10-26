package com.wonnabe.classifier.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wonnabe.classifier.domain.ClassificationMethod;
import com.wonnabe.classifier.domain.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	List<Transaction> findByClassificationMethod(ClassificationMethod classificationMethod);
}
