package com.wonnabe.classifier.classification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.wonnabe.classifier.domain.Category;
import com.wonnabe.classifier.domain.ClassificationMethod;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordClassifierTest {

	private final KeywordClassifier classifier = new KeywordClassifier();

	@ParameterizedTest
	@CsvSource({
			"스타벅스 강남점, , FOOD",
			"맥도날드 역삼점, , FOOD",
			"올리브영, , SHOPPING",
			"카카오T일반택시(법인)_4, 택시, TRANSPORT",
			"CGV 여의도, , CULTURE",
			"야놀자, , TRAVEL"
	})
	void 가맹점명이나_업종에_키워드가_있으면_해당_카테고리로_분류한다(String merchantName, String storeType, Category expected) {
		ClassificationResult result = classifier.classify(merchantName, storeType);

		assertThat(result.category()).isEqualTo(expected);
		assertThat(result.method()).isEqualTo(ClassificationMethod.KEYWORD);
	}

	@Test
	void 가맹점명이_아니라_업종에만_키워드가_있어도_분류한다() {
		ClassificationResult result = classifier.classify("디파인", "안경원 호텔라운지");

		assertThat(result.category()).isEqualTo(Category.TRAVEL);
	}

	@Test
	void 어떤_키워드와도_매칭되지_않으면_UNCLASSIFIED를_반환한다() {
		ClassificationResult result = classifier.classify("엔에이치엔케이씨피 주식회사", "PG일반(인증)");

		assertThat(result.category()).isNull();
		assertThat(result.method()).isEqualTo(ClassificationMethod.UNCLASSIFIED);
	}

	@Test
	void 업종이_null이어도_예외없이_UNCLASSIFIED를_반환한다() {
		ClassificationResult result = classifier.classify("알수없는가게", null);

		assertThat(result.method()).isEqualTo(ClassificationMethod.UNCLASSIFIED);
	}

	@Test
	void 대소문자를_구분하지_않는다() {
		ClassificationResult result = classifier.classify("STARBUCKS", "HOTEL 라운지");

		assertThat(result.category()).isEqualTo(Category.TRAVEL);
	}

	@Test
	void 여러_카테고리에_걸리면_KeywordRules에_먼저_선언된_카테고리가_우선한다() {
		// "카페"(FOOD)가 "공항"(TRANSPORT)보다 RULES에 먼저 선언되어 있음
		ClassificationResult result = classifier.classify("공항카페점", null);

		assertThat(result.category()).isEqualTo(Category.FOOD);
	}
}
