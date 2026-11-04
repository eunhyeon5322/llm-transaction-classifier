package com.wonnabe.classifier.classification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.wonnabe.classifier.domain.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gemini API 실제 호출(RestClient)은 네트워크가 필요해 단위 테스트 대상에서 제외하고,
 * 응답 텍스트를 Category로 바꾸는 파싱 로직만 검증한다 — 실제 호출은 수동으로 /reclassify를
 * 돌려가며 검증했다(docs/experiments.md, Notion "분류 결과 테스트" 참고).
 */
class GeminiClassificationServiceTest {

	private final GeminiClassificationService service =
			new GeminiClassificationService("https://example.invalid", "test-model", "test-key");

	@ParameterizedTest
	@ValueSource(strings = {"food", "transport", "shopping", "culture", "travel", "other"})
	void 모델이_정의된_카테고리_코드를_그대로_주면_해당_Category로_변환한다(String code) {
		Category category = service.parseCategory(code);

		assertThat(category).isEqualTo(Category.valueOf(code.toUpperCase()));
	}

	@Test
	void 대소문자나_앞뒤_공백은_무시하고_파싱한다() {
		assertThat(service.parseCategory("  FOOD  ")).isEqualTo(Category.FOOD);
		assertThat(service.parseCategory("Travel")).isEqualTo(Category.TRAVEL);
	}

	@Test
	void 정의되지_않은_카테고리_코드는_null을_반환한다() {
		assertThat(service.parseCategory("entertainment")).isNull();
		assertThat(service.parseCategory("죄송합니다, 분류할 수 없습니다.")).isNull();
	}

	@Test
	void 응답_텍스트가_null이면_null을_반환한다() {
		assertThat(service.parseCategory(null)).isNull();
	}
}
