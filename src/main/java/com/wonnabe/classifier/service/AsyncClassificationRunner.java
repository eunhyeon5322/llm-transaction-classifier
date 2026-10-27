package com.wonnabe.classifier.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.wonnabe.classifier.classification.ClassificationResult;
import com.wonnabe.classifier.classification.GeminiClassificationService;
import com.wonnabe.classifier.domain.Transaction;

import lombok.RequiredArgsConstructor;

/**
 * ReclassificationService와 별도 빈으로 분리한 이유:
 * 같은 빈 안에서 this::classifyAsync로 자기 자신의 @Async 메서드를 호출하면
 * 스프링 AOP 프록시를 거치지 않아 @Async가 적용되지 않는다(자가 호출 문제).
 * 호출하는 쪽(ReclassificationService)과 호출받는 쪽을 다른 빈으로 나눠서
 * 반드시 프록시를 통해 호출되도록 함.
 */
@Component
@RequiredArgsConstructor
public class AsyncClassificationRunner {

	private final GeminiClassificationService geminiClassificationService;

	@Async("llmTaskExecutor")
	public CompletableFuture<ClassificationResult> classify(Transaction transaction) {
		ClassificationResult result = geminiClassificationService.classify(
				transaction.getMerchantName(), transaction.getMerchantStoreType());
		return CompletableFuture.completedFuture(result);
	}
}
