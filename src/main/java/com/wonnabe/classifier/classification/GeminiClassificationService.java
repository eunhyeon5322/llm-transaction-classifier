package com.wonnabe.classifier.classification;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.wonnabe.classifier.domain.Category;
import com.wonnabe.classifier.domain.ClassificationMethod;

/**
 * 키워드로 못 잡은 가맹점명을 Gemini API로 2차 분류한다.
 * 같은 가맹점명에 대한 결과는 Redis에 캐싱되어, 동일 가맹점명 재호출 시 API를 다시 부르지 않는다.
 */
@Service
public class GeminiClassificationService {

	private static final Logger log = LoggerFactory.getLogger(GeminiClassificationService.class);

	private static final String SYSTEM_INSTRUCTION = """
			You are a classifier for Korean card transaction merchant names.
			Given a merchant name (and optionally its business type), respond with exactly ONE of
			these category codes and nothing else - no punctuation, no explanation, lowercase only:
			food, transport, shopping, culture, other.
			If the merchant name gives no clue which category it belongs to, respond with other.
			""";

	private final RestClient restClient;
	private final String model;
	private final String apiKey;

	public GeminiClassificationService(
			@Value("${gemini.base-url}") String baseUrl,
			@Value("${gemini.model}") String model,
			@Value("${gemini.api-key}") String apiKey) {
		this.restClient = RestClient.builder().baseUrl(baseUrl).build();
		this.model = model;
		this.apiKey = apiKey;
	}

	@Cacheable(value = "merchantCategory", key = "#merchantName + '|' + #merchantStoreType")
	public ClassificationResult classify(String merchantName, String merchantStoreType) {
		String userText = "가맹점명: " + merchantName
				+ (merchantStoreType == null || merchantStoreType.isBlank() ? "" : "\n업종: " + merchantStoreType);

		GeminiRequest request = new GeminiRequest(
				new GeminiRequest.SystemInstruction(List.of(new GeminiRequest.Part(SYSTEM_INSTRUCTION))),
				List.of(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(userText))))
		);

		try {
			GeminiResponse response = restClient.post()
					.uri("/models/{model}:generateContent", model)
					.header("x-goog-api-key", apiKey)
					.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(GeminiResponse.class);

			String text = response.candidates().get(0).content().parts().get(0).text();
			Category category = parseCategory(text);
			if (category == null) {
				log.warn("Gemini returned unparseable category '{}' for merchant '{}'", text, merchantName);
				return new ClassificationResult(null, ClassificationMethod.UNCLASSIFIED);
			}
			return new ClassificationResult(category, ClassificationMethod.LLM);
		} catch (Exception e) {
			log.warn("Gemini classification failed for merchant '{}': {}", merchantName, e.getMessage());
			return new ClassificationResult(null, ClassificationMethod.UNCLASSIFIED);
		}
	}

	private Category parseCategory(String text) {
		if (text == null) {
			return null;
		}
		String normalized = text.trim().toLowerCase(Locale.ROOT);
		for (Category category : Category.values()) {
			if (category.name().toLowerCase(Locale.ROOT).equals(normalized)) {
				return category;
			}
		}
		return null;
	}
}
