package com.wonnabe.classifier.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.wonnabe.classifier.classification.ClassificationResult;
import com.wonnabe.classifier.classification.KeywordClassifier;
import com.wonnabe.classifier.domain.Category;
import com.wonnabe.classifier.domain.ClassificationMethod;
import com.wonnabe.classifier.domain.Transaction;
import com.wonnabe.classifier.dto.response.UploadResultResponse;
import com.wonnabe.classifier.repository.TransactionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionUploadServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private KeywordClassifier keywordClassifier;

	private TransactionUploadService service;

	private MockMultipartFile csv(String content) {
		return new MockMultipartFile("file", "card_transactions.csv", "text/csv",
				content.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void 정상_행은_키워드_분류_결과와_함께_저장된다() {
		service = new TransactionUploadService(transactionRepository, keywordClassifier);
		when(keywordClassifier.classify(anyString(), any()))
				.thenReturn(new ClassificationResult(Category.FOOD, ClassificationMethod.KEYWORD));

		String content = """
				transaction_date,merchant_name,merchant_store_type,amount
				2026-10-01,스타벅스 강남점,카페,4500
				2026-10-02,올리브영,드럭스토어,12000
				""";

		UploadResultResponse response = service.upload(csv(content));

		assertThat(response.savedCount()).isEqualTo(2);
		assertThat(response.skippedCount()).isZero();

		ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
		verify(transactionRepository).saveAll(captor.capture());
		List<Transaction> saved = captor.getValue();
		assertThat(saved).hasSize(2);
		assertThat(saved.get(0).getMerchantName()).isEqualTo("스타벅스 강남점");
		assertThat(saved.get(0).getCategory()).isEqualTo(Category.FOOD);
		assertThat(saved.get(0).getClassificationMethod()).isEqualTo(ClassificationMethod.KEYWORD);
	}

	@Test
	void amount가_숫자가_아닌_행은_건너뛰고_skippedCount에_반영한다() {
		service = new TransactionUploadService(transactionRepository, keywordClassifier);
		when(keywordClassifier.classify(anyString(), any()))
				.thenReturn(new ClassificationResult(null, ClassificationMethod.UNCLASSIFIED));

		String content = """
				transaction_date,merchant_name,merchant_store_type,amount
				2026-10-01,정상가게,일반음식점,4500
				2026-10-02,금액이상한가게,일반음식점,이천원
				""";

		UploadResultResponse response = service.upload(csv(content));

		assertThat(response.savedCount()).isEqualTo(1);
		assertThat(response.skippedCount()).isEqualTo(1);
	}

	@Test
	void merchant_store_type_컬럼이_없으면_null로_저장되고_예외없이_처리된다() {
		service = new TransactionUploadService(transactionRepository, keywordClassifier);
		when(keywordClassifier.classify(anyString(), any()))
				.thenReturn(new ClassificationResult(null, ClassificationMethod.UNCLASSIFIED));

		String content = """
				transaction_date,merchant_name,amount
				2026-10-01,업종정보없는가게,4500
				""";

		UploadResultResponse response = service.upload(csv(content));

		assertThat(response.savedCount()).isEqualTo(1);

		ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
		verify(transactionRepository).saveAll(captor.capture());
		assertThat(captor.getValue().get(0).getMerchantStoreType()).isNull();
	}
}
