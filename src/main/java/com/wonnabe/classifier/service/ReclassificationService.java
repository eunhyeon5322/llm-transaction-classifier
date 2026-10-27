package com.wonnabe.classifier.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.wonnabe.classifier.classification.ClassificationResult;
import com.wonnabe.classifier.domain.ClassificationMethod;
import com.wonnabe.classifier.domain.Transaction;
import com.wonnabe.classifier.dto.response.ReclassifyResultResponse;
import com.wonnabe.classifier.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

/**
 * 키워드로 분류되지 않은(UNCLASSIFIED) 거래를 Gemini API로 2차 분류한다.
 * 여러 건을 순서대로 하나씩 호출하면 느려서(건당 API 응답을 기다려야 함),
 * 전용 스레드풀(llmTaskExecutor, 5개)로 동시에 여러 건을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class ReclassificationService {

	private static final Logger log = LoggerFactory.getLogger(ReclassificationService.class);

	private final TransactionRepository transactionRepository;
	private final AsyncClassificationRunner asyncClassificationRunner;

	public ReclassifyResultResponse reclassifyUnclassified() {
		List<Transaction> targets = transactionRepository.findByClassificationMethod(ClassificationMethod.UNCLASSIFIED);
		log.info("reclassifyUnclassified: found {} targets", targets.size());

		List<CompletableFuture<ClassificationResult>> futures = targets.stream()
				.map(asyncClassificationRunner::classify)
				.toList();

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		int reclassified = 0;
		for (int i = 0; i < targets.size(); i++) {
			ClassificationResult result = futures.get(i).join();
			if (result.method() == ClassificationMethod.LLM) {
				targets.get(i).classify(result.category(), result.method());
				reclassified++;
			}
		}
		transactionRepository.saveAll(targets);

		return new ReclassifyResultResponse(targets.size(), reclassified, targets.size() - reclassified);
	}
}
