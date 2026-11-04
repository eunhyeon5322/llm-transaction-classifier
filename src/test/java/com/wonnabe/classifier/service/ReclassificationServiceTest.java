package com.wonnabe.classifier.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wonnabe.classifier.classification.ClassificationResult;
import com.wonnabe.classifier.domain.Category;
import com.wonnabe.classifier.domain.ClassificationMethod;
import com.wonnabe.classifier.domain.Transaction;
import com.wonnabe.classifier.dto.response.ReclassifyResultResponse;
import com.wonnabe.classifier.repository.TransactionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReclassificationServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private AsyncClassificationRunner asyncClassificationRunner;

	private Transaction unclassifiedTransaction(String merchantName) {
		return Transaction.builder()
				.merchantName(merchantName)
				.merchantStoreType(null)
				.amount(BigDecimal.TEN)
				.transactionDate(LocalDate.of(2026, 10, 1))
				.category(null)
				.classificationMethod(ClassificationMethod.UNCLASSIFIED)
				.build();
	}

	@Test
	void LLM_분류에_성공한_건만_카테고리를_갱신하고_실패한_건은_UNCLASSIFIED로_남긴다() {
		Transaction success = unclassifiedTransaction("스타벅스 강남점");
		Transaction stillFailing = unclassifiedTransaction("정체불명가게");

		when(transactionRepository.findByClassificationMethod(ClassificationMethod.UNCLASSIFIED))
				.thenReturn(List.of(success, stillFailing));
		when(asyncClassificationRunner.classify(success))
				.thenReturn(CompletableFuture.completedFuture(
						new ClassificationResult(Category.FOOD, ClassificationMethod.LLM)));
		when(asyncClassificationRunner.classify(stillFailing))
				.thenReturn(CompletableFuture.completedFuture(
						new ClassificationResult(null, ClassificationMethod.UNCLASSIFIED)));

		ReclassificationService service = new ReclassificationService(transactionRepository, asyncClassificationRunner);
		ReclassifyResultResponse response = service.reclassifyUnclassified();

		assertThat(response.attempted()).isEqualTo(2);
		assertThat(response.reclassified()).isEqualTo(1);
		assertThat(response.stillUnclassified()).isEqualTo(1);

		assertThat(success.getCategory()).isEqualTo(Category.FOOD);
		assertThat(success.getClassificationMethod()).isEqualTo(ClassificationMethod.LLM);
		assertThat(stillFailing.getClassificationMethod()).isEqualTo(ClassificationMethod.UNCLASSIFIED);
	}

	@Test
	void 재분류_대상이_없으면_0건으로_응답한다() {
		when(transactionRepository.findByClassificationMethod(ClassificationMethod.UNCLASSIFIED))
				.thenReturn(List.of());

		ReclassificationService service = new ReclassificationService(transactionRepository, asyncClassificationRunner);
		ReclassifyResultResponse response = service.reclassifyUnclassified();

		assertThat(response.attempted()).isZero();
		assertThat(response.reclassified()).isZero();
		assertThat(response.stillUnclassified()).isZero();
	}
}
