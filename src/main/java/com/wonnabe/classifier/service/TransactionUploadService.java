package com.wonnabe.classifier.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.wonnabe.classifier.classification.ClassificationResult;
import com.wonnabe.classifier.classification.KeywordClassifier;
import com.wonnabe.classifier.domain.Transaction;
import com.wonnabe.classifier.dto.response.UploadResultResponse;
import com.wonnabe.classifier.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionUploadService {

	private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.setIgnoreHeaderCase(true)
			.setTrim(true)
			.build();

	private final TransactionRepository transactionRepository;
	private final KeywordClassifier keywordClassifier;

	public UploadResultResponse upload(MultipartFile file) {
		List<Transaction> transactions = new ArrayList<>();
		int skippedCount = 0;

		try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
			 CSVParser parser = CSV_FORMAT.parse(reader)) {
			for (CSVRecord record : parser) {
				Transaction transaction = parseRow(record);
				if (transaction != null) {
					transactions.add(transaction);
				} else {
					skippedCount++;
				}
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("CSV 파일을 읽을 수 없습니다.", e);
		}

		transactionRepository.saveAll(transactions);
		return new UploadResultResponse(transactions.size(), skippedCount);
	}

	private Transaction parseRow(CSVRecord record) {
		try {
			BigDecimal amount = new BigDecimal(record.get("amount").trim());
			LocalDate transactionDate = LocalDate.parse(record.get("transaction_date").trim());
			String merchantName = record.isMapped("merchant_name") ? record.get("merchant_name").trim() : "";
			String merchantStoreType = record.isMapped("merchant_store_type") ? record.get("merchant_store_type").trim() : null;

			ClassificationResult result = keywordClassifier.classify(merchantName, merchantStoreType);

			return Transaction.builder()
					.merchantName(merchantName)
					.merchantStoreType(merchantStoreType)
					.amount(amount)
					.transactionDate(transactionDate)
					.category(result.category())
					.classificationMethod(result.method())
					.build();
		} catch (Exception e) {
			return null;
		}
	}
}
