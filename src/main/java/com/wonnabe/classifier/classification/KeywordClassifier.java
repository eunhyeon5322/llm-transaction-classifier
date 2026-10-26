package com.wonnabe.classifier.classification;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.wonnabe.classifier.domain.Category;
import com.wonnabe.classifier.domain.ClassificationMethod;

/**
 * 가맹점명/업종에 키워드가 포함되어 있으면 해당 카테고리로 분류한다 (1차 분류).
 * 어떤 키워드와도 매칭되지 않으면 UNCLASSIFIED를 반환하며, 이후 Claude API(2차 분류) 대상이 된다.
 */
@Component
public class KeywordClassifier {

	public ClassificationResult classify(String merchantName, String merchantStoreType) {
		String text = normalize(merchantName) + " " + normalize(merchantStoreType);

		for (Map.Entry<Category, java.util.List<String>> entry : KeywordRules.RULES.entrySet()) {
			for (String keyword : entry.getValue()) {
				if (text.contains(keyword)) {
					return new ClassificationResult(entry.getKey(), ClassificationMethod.KEYWORD);
				}
			}
		}
		return ClassificationResult.UNCLASSIFIED;
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase();
	}
}
